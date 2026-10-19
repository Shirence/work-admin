package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.chat.MessageDtos;
import com.example.workadmin.entity.ChatConversation;
import com.example.workadmin.entity.ChatDevice;
import com.example.workadmin.entity.ChatEnvelope;
import com.example.workadmin.entity.ChatMember;
import com.example.workadmin.entity.ChatMessage;
import com.example.workadmin.repository.ChatConversationRepository;
import com.example.workadmin.repository.ChatDeviceRepository;
import com.example.workadmin.repository.ChatEnvelopeRepository;
import com.example.workadmin.repository.ChatMemberRepository;
import com.example.workadmin.repository.ChatMessageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息发送与多端同步。
 *
 * <p>发送流程在一个数据库事务内完成：</p>
 * <ol>
 *   <li>校验发送设备为 ACTIVE 且是会话成员；</li>
 *   <li>锁定会话行，串行分配 serverSeq；</li>
 *   <li>按幂等键查重，重复请求直接返回已存在的 serverSeq；</li>
 *   <li>校验每个目标设备都是会话成员的 ACTIVE 设备，且覆盖所有应当接收的设备；</li>
 *   <li>写入消息元数据和逐设备密文信封。</li>
 * </ol>
 */
@Service
public class ChatMessageService {

    private static final int MAX_PAGE_SIZE = 500;

    private final ChatMessageRepository messageRepository;
    private final ChatEnvelopeRepository envelopeRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMemberRepository memberRepository;
    private final ChatDeviceRepository deviceRepository;
    private final ChatDeviceService deviceService;
    private final ChatConversationService conversationService;

    public ChatMessageService(ChatMessageRepository messageRepository,
                              ChatEnvelopeRepository envelopeRepository,
                              ChatConversationRepository conversationRepository,
                              ChatMemberRepository memberRepository,
                              ChatDeviceRepository deviceRepository,
                              ChatDeviceService deviceService,
                              ChatConversationService conversationService) {
        this.messageRepository = messageRepository;
        this.envelopeRepository = envelopeRepository;
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.deviceRepository = deviceRepository;
        this.deviceService = deviceService;
        this.conversationService = conversationService;
    }

    @Transactional
    public MessageDtos.SendResult send(String username, MessageDtos.SendRequest request) {
        ChatDevice sender = deviceService.requireActiveDevice(username, request.senderDeviceKey());

        ChatConversation conversation = conversationRepository.findByIdForUpdate(request.conversationId())
                .orElseThrow(() -> new BizException(404, "会话不存在"));
        memberRepository.findByConversationIdAndUsername(conversation.getId(), username)
                .orElseThrow(() -> new BizException(403, "你不是该会话成员"));

        if (request.expectedVersion() != null
                && !request.expectedVersion().equals(conversation.getVersion())) {
            throw new BizException(409, "会话成员或信息已变更，请刷新后重试");
        }

        Optional<ChatMessage> existing = messageRepository
                .findByConversationIdAndSenderUsernameAndSenderClientDeviceKeyAndClientMessageId(
                        conversation.getId(), username, sender.getClientDeviceKey(), request.clientMessageId().trim());
        if (existing.isPresent()) {
            ChatMessage message = existing.get();
            return new MessageDtos.SendResult(
                    true,
                    toMessageDto(message, (int) envelopeRepository.countByMessageId(message.getId()), null),
                    conversation.getVersion()
            );
        }

        List<ChatMember> members = memberRepository
                .findByConversationIdOrderByJoinedAtAsc(conversation.getId());
        Set<String> memberUsernames = members.stream()
                .map(ChatMember::getUsername)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ChatDevice> activeDevices = deviceRepository
                .findByUsernameInAndStatusOrderByUsernameAscDeviceIdAsc(memberUsernames, "ACTIVE");
        Map<String, ChatDevice> devicesByKey = activeDevices.stream()
                .collect(Collectors.toMap(ChatDevice::getClientDeviceKey, Function.identity(), (a, b) -> a));

        Set<String> requiredTargets = activeDevices.stream()
                .map(ChatDevice::getClientDeviceKey)
                .filter(key -> !key.equals(sender.getClientDeviceKey()))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, MessageDtos.EnvelopeRequest> provided = new LinkedHashMap<>();
        if (request.envelopes() != null) {
            for (MessageDtos.EnvelopeRequest envelope : request.envelopes()) {
                String key = envelope.targetDeviceKey() == null ? "" : envelope.targetDeviceKey().trim();
                if (key.isEmpty()) {
                    throw new BizException(400, "envelope.targetDeviceKey 不能为空");
                }
                if (provided.putIfAbsent(key, envelope) != null) {
                    throw new BizException(400, "同一目标设备重复提交了消息副本：" + key);
                }
                if (envelope.body() == null || envelope.body().isBlank()) {
                    throw new BizException(400, "消息密文不能为空");
                }
                if (envelope.body().length() > 2_000_000) {
                    throw new BizException(400, "单条消息密文过大");
                }
            }
        }

        Set<String> missing = new LinkedHashSet<>(requiredTargets);
        missing.removeAll(provided.keySet());
        if (!missing.isEmpty()) {
            throw new BizException(400, "缺少 " + missing.size() + " 台设备的消息副本，请刷新设备列表后重试");
        }
        Set<String> invalid = new LinkedHashSet<>(provided.keySet());
        invalid.removeAll(requiredTargets);
        if (!invalid.isEmpty()) {
            throw new BizException(400, "目标设备未激活、不是会话成员设备或不应接收该消息");
        }

        long nextSeq = (conversation.getLastMessageSeq() == null ? 0L : conversation.getLastMessageSeq()) + 1;
        LocalDateTime now = LocalDateTime.now();

        ChatMessage message = new ChatMessage();
        message.setConversationId(conversation.getId());
        message.setSenderUsername(username);
        message.setSenderDeviceId(sender.getDeviceId());
        message.setSenderClientDeviceKey(sender.getClientDeviceKey());
        message.setClientMessageId(request.clientMessageId().trim());
        message.setServerSeq(nextSeq);
        message.setContentType(normalizeContentType(request.contentType()));
        message.setCreatedAt(now);
        message = messageRepository.save(message);

        List<ChatEnvelope> envelopes = new ArrayList<>(provided.size());
        for (Map.Entry<String, MessageDtos.EnvelopeRequest> entry : provided.entrySet()) {
            ChatDevice target = devicesByKey.get(entry.getKey());
            MessageDtos.EnvelopeRequest payload = entry.getValue();
            ChatEnvelope envelope = new ChatEnvelope();
            envelope.setMessageId(message.getId());
            envelope.setConversationId(conversation.getId());
            envelope.setTargetUsername(target.getUsername());
            envelope.setTargetDeviceId(target.getDeviceId());
            envelope.setTargetDeviceKey(target.getClientDeviceKey());
            envelope.setCipherType(payload.cipherType());
            envelope.setBody(payload.body().trim());
            envelope.setRegistrationId(target.getRegistrationId());
            envelope.setCreatedAt(now);
            envelopes.add(envelope);
        }
        envelopeRepository.saveAll(envelopes);

        conversation.setLastMessageSeq(nextSeq);
        conversation.setLastMessageAt(now);
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);

        return new MessageDtos.SendResult(
                false,
                toMessageDto(message, envelopes.size(), null),
                conversation.getVersion()
        );
    }

    /**
     * 拉取某会话在 afterSeq 之后、发给指定设备的密文消息。
     * 只有 ACTIVE 设备可调用；新设备授权前无法同步历史。
     */
    @Transactional
    public List<MessageDtos.MessageDto> sync(String username,
                                           Long conversationId,
                                           String deviceKey,
                                           Long afterSeq,
                                           Integer limit) {
        ChatDevice device = deviceService.requireActiveDevice(username, deviceKey);
        ChatConversation conversation = conversationService.requireConversation(conversationId);
        conversationService.requireMember(conversation.getId(), username);

        long after = afterSeq == null ? 0L : Math.max(0L, afterSeq);
        int size = limit == null ? 100 : Math.min(MAX_PAGE_SIZE, Math.max(1, limit));
        List<ChatMessage> messages = messageRepository
                .findByConversationIdAndServerSeqGreaterThanOrderByServerSeqAsc(
                        conversationId, after, PageRequest.of(0, size));
        return toMessageDtos(messages, device.getClientDeviceKey());
    }

    /**
     * 历史消息分页（向更早的 serverSeq 翻页），仍然只返回当前设备的信封。
     */
    @Transactional
    public List<MessageDtos.MessageDto> history(String username,
                                                Long conversationId,
                                                String deviceKey,
                                                Integer limit) {
        ChatDevice device = deviceService.requireActiveDevice(username, deviceKey);
        conversationService.requireConversation(conversationId);
        conversationService.requireMember(conversationId, username);
        int size = limit == null ? 50 : Math.min(MAX_PAGE_SIZE, Math.max(1, limit));
        List<ChatMessage> messages = messageRepository
                .findByConversationIdOrderByServerSeqDesc(conversationId, PageRequest.of(0, size));
        List<MessageDtos.MessageDto> result = new ArrayList<>(toMessageDtos(messages, device.getClientDeviceKey()));
        result.sort((a, b) -> Long.compare(a.serverSeq(), b.serverSeq()));
        return result;
    }

    private List<MessageDtos.MessageDto> toMessageDtos(List<ChatMessage> messages, String deviceKey) {
        if (messages.isEmpty()) {
            return List.of();
        }
        List<Long> ids = messages.stream().map(ChatMessage::getId).toList();
        Map<Long, List<ChatEnvelope>> envelopesByMessage = envelopeRepository.findByMessageIdIn(ids)
                .stream()
                .collect(Collectors.groupingBy(ChatEnvelope::getMessageId));

        List<MessageDtos.MessageDto> result = new ArrayList<>(messages.size());
        for (ChatMessage message : messages) {
            List<ChatEnvelope> all = envelopesByMessage.getOrDefault(message.getId(), List.of());
            ChatEnvelope mine = all.stream()
                    .filter(e -> e.getTargetDeviceKey().equals(deviceKey))
                    .findFirst()
                    .orElse(null);
            result.add(toMessageDto(message, all.size(), mine == null ? null : toEnvelopeDto(mine)));
        }
        return result;
    }

    private MessageDtos.MessageDto toMessageDto(ChatMessage message,
                                                int envelopeCount,
                                                MessageDtos.EnvelopeDto envelope) {
        return new MessageDtos.MessageDto(
                message.getId(),
                message.getConversationId(),
                message.getServerSeq(),
                message.getSenderUsername(),
                message.getSenderDeviceId(),
                message.getSenderClientDeviceKey(),
                message.getClientMessageId(),
                message.getContentType(),
                message.getCreatedAt(),
                envelopeCount,
                envelope
        );
    }

    private MessageDtos.EnvelopeDto toEnvelopeDto(ChatEnvelope envelope) {
        return new MessageDtos.EnvelopeDto(
                envelope.getTargetDeviceKey(),
                envelope.getCipherType(),
                envelope.getBody(),
                envelope.getRegistrationId()
        );
    }

    private String normalizeContentType(String contentType) {
        String value = contentType == null ? "" : contentType.trim().toUpperCase();
        if (value.isEmpty()) {
            return "TEXT";
        }
        if (value.length() > 20) {
            throw new BizException(400, "contentType 过长");
        }
        return value;
    }
}
