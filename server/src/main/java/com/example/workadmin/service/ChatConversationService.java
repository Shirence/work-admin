package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.ChatConstants;
import com.example.workadmin.dto.chat.ConversationDtos;
import com.example.workadmin.entity.ChatConversation;
import com.example.workadmin.entity.ChatDevice;
import com.example.workadmin.entity.ChatMember;
import com.example.workadmin.entity.ChatReadCursor;
import com.example.workadmin.repository.AccountRepository;
import com.example.workadmin.repository.ChatConversationRepository;
import com.example.workadmin.repository.ChatDeviceLinkRepository;
import com.example.workadmin.repository.ChatMemberRepository;
import com.example.workadmin.repository.ChatReadCursorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 会话与群成员管理。
 *
 * <p>私聊由排序后的两个 username 唯一确定；群聊由有建群权限的账号创建，
 * 创建者为 OWNER。成员/名称变化时 version 自增，客户端据此重新拉取成员和
 * 设备密钥包。</p>
 */
@Service
public class ChatConversationService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMemberRepository memberRepository;
    private final ChatReadCursorRepository readCursorRepository;
    private final ChatDeviceLinkRepository deviceLinkRepository;
    private final AccountRepository accountRepository;
    private final ChatDeviceService deviceService;
    private final ChatGroupPolicyService groupPolicyService;

    public ChatConversationService(ChatConversationRepository conversationRepository,
                                   ChatMemberRepository memberRepository,
                                   ChatReadCursorRepository readCursorRepository,
                                   ChatDeviceLinkRepository deviceLinkRepository,
                                   AccountRepository accountRepository,
                                   ChatDeviceService deviceService,
                                   ChatGroupPolicyService groupPolicyService) {
        this.conversationRepository = conversationRepository;
        this.memberRepository = memberRepository;
        this.readCursorRepository = readCursorRepository;
        this.deviceLinkRepository = deviceLinkRepository;
        this.accountRepository = accountRepository;
        this.deviceService = deviceService;
        this.groupPolicyService = groupPolicyService;
    }

    @Transactional
    public ConversationDtos.ConversationDto createPrivate(String username, String targetUsername) {
        String target = targetUsername == null ? "" : targetUsername.trim();
        if (target.isEmpty()) {
            throw new BizException(400, "缺少对方账号");
        }
        if (target.equals(username)) {
            throw new BizException(400, "不能与自己创建私聊");
        }
        if (!accountRepository.existsByUsername(target)) {
            throw new BizException(404, "账号不存在：" + target);
        }

        String key = privateKey(username, target);
        ChatConversation conversation = conversationRepository.findByPrivateKey(key).orElse(null);
        if (conversation == null) {
            LocalDateTime now = LocalDateTime.now();
            conversation = new ChatConversation();
            conversation.setType(ChatConstants.CONVERSATION_PRIVATE);
            conversation.setPrivateKey(key);
            conversation.setVersion(1L);
            conversation.setLastMessageSeq(0L);
            conversation.setCreatedAt(now);
            conversation.setUpdatedAt(now);
            conversation = conversationRepository.save(conversation);
            addMember(conversation.getId(), username, ChatConstants.ROLE_MEMBER, now);
            addMember(conversation.getId(), target, ChatConstants.ROLE_MEMBER, now);
        } else {
            ensureMember(conversation.getId(), username);
            ensureMember(conversation.getId(), target);
        }
        return toDtoForUser(conversation, username);
    }

    @Transactional
    public ConversationDtos.ConversationDto createGroup(String username,
                                                        String name,
                                                        List<String> memberUsernames) {
        groupPolicyService.assertCanCreateGroup(username);
        String groupName = name == null ? "" : name.trim();
        if (groupName.isEmpty() || groupName.length() > 80) {
            throw new BizException(400, "群名称长度必须为 1-80 个字符");
        }
        Set<String> members = new LinkedHashSet<>();
        if (memberUsernames != null) {
            for (String raw : memberUsernames) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String member = raw.trim();
                if (!member.equals(username)) {
                    members.add(member);
                }
            }
        }
        if (members.isEmpty()) {
            throw new BizException(400, "群聊至少需要邀请一位其他成员");
        }
        if (members.size() + 1 > ChatConstants.MAX_GROUP_MEMBERS) {
            throw new BizException(400, "群成员数量不能超过 " + ChatConstants.MAX_GROUP_MEMBERS);
        }
        for (String member : members) {
            if (!accountRepository.existsByUsername(member)) {
                throw new BizException(404, "账号不存在：" + member);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        ChatConversation conversation = new ChatConversation();
        conversation.setType(ChatConstants.CONVERSATION_GROUP);
        conversation.setName(groupName);
        conversation.setOwnerUsername(username);
        conversation.setVersion(1L);
        conversation.setLastMessageSeq(0L);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversation = conversationRepository.save(conversation);

        addMember(conversation.getId(), username, ChatConstants.ROLE_OWNER, now);
        for (String member : members) {
            addMember(conversation.getId(), member, ChatConstants.ROLE_MEMBER, now);
        }
        return toDtoForUser(conversation, username);
    }

    @Transactional
    public List<ConversationDtos.ConversationDto> list(String username, String deviceKey) {
        String activeDeviceKey = requireOptionalActiveDevice(username, deviceKey);
        List<ChatMember> memberships = memberRepository.findByUsernameOrderByJoinedAtAsc(username);
        if (memberships.isEmpty()) {
            return List.of();
        }
        List<Long> ids = memberships.stream().map(ChatMember::getConversationId).toList();
        Map<Long, ChatConversation> conversations = conversationRepository.findByIdIn(ids)
                .stream()
                .collect(Collectors.toMap(ChatConversation::getId, Function.identity()));
        Map<Long, List<ChatMember>> membersByConversation = memberRepository
                .findByConversationIdInOrderByJoinedAtAsc(ids)
                .stream()
                .collect(Collectors.groupingBy(ChatMember::getConversationId));
        Map<Long, ChatReadCursor> cursors = readCursorRepository
                .findByUsernameAndDeviceKey(username, activeDeviceKey)
                .stream()
                .collect(Collectors.toMap(ChatReadCursor::getConversationId, Function.identity(), (a, b) -> a));

        List<ConversationDtos.ConversationDto> result = new ArrayList<>();
        for (ChatMember membership : memberships) {
            ChatConversation conversation = conversations.get(membership.getConversationId());
            if (conversation == null) {
                continue;
            }
            List<ChatMember> members = membersByConversation.getOrDefault(conversation.getId(), List.of());
            long readSeq = resolveReadSeq(membership, cursors.get(conversation.getId()), activeDeviceKey != null);
            result.add(toDto(conversation, members, membership.getRole(), readSeq));
        }
        result.sort(Comparator
                .comparing(ConversationDtos.ConversationDto::lastMessageAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ConversationDtos.ConversationDto::updatedAt, Comparator.reverseOrder()));
        return result;
    }

    @Transactional
    public ConversationDtos.ConversationDto get(String username, Long conversationId, String deviceKey) {
        ChatConversation conversation = requireConversation(conversationId);
        ChatMember membership = requireMember(conversationId, username);
        String activeDeviceKey = requireOptionalActiveDevice(username, deviceKey);
        long readSeq = activeDeviceKey == null
                ? (membership.getLastReadSeq() == null ? 0L : membership.getLastReadSeq())
                : readCursorRepository.findByConversationIdAndDeviceKey(conversationId, activeDeviceKey)
                .map(ChatReadCursor::getLastReadSeq)
                .orElse(0L);
        return toDto(conversation, memberRepository.findByConversationIdOrderByJoinedAtAsc(conversationId),
                membership.getRole(), readSeq);
    }

    @Transactional
    public ConversationDtos.ConversationDto addMembers(String username,
                                                       Long conversationId,
                                                       List<String> usernames) {
        ChatConversation conversation = requireGroup(conversationId);
        requireOwner(conversation, username);
        if (usernames == null || usernames.isEmpty()) {
            throw new BizException(400, "请选择要添加的成员");
        }
        Set<String> targets = new LinkedHashSet<>();
        for (String raw : usernames) {
            if (raw != null && !raw.isBlank()) {
                targets.add(raw.trim());
            }
        }
        targets.remove(username);
        if (targets.isEmpty()) {
            throw new BizException(400, "请选择要添加的成员");
        }
        long currentCount = memberRepository.countByConversationId(conversationId);
        if (currentCount + targets.size() > ChatConstants.MAX_GROUP_MEMBERS) {
            throw new BizException(400, "群成员数量不能超过 " + ChatConstants.MAX_GROUP_MEMBERS);
        }
        LocalDateTime now = LocalDateTime.now();
        for (String target : targets) {
            if (!accountRepository.existsByUsername(target)) {
                throw new BizException(404, "账号不存在：" + target);
            }
            if (!memberRepository.existsByConversationIdAndUsername(conversationId, target)) {
                addMember(conversationId, target, ChatConstants.ROLE_MEMBER, now);
            }
        }
        bumpVersion(conversation, now);
        return toDtoForUser(conversation, username);
    }

    @Transactional
    public ConversationDtos.ConversationDto removeMember(String username,
                                                         Long conversationId,
                                                         String targetUsername) {
        ChatConversation conversation = requireGroup(conversationId);
        requireOwner(conversation, username);
        String target = targetUsername == null ? "" : targetUsername.trim();
        if (target.equals(conversation.getOwnerUsername())) {
            throw new BizException(400, "不能移除群主");
        }
        ChatMember member = memberRepository.findByConversationIdAndUsername(conversationId, target)
                .orElseThrow(() -> new BizException(404, "该成员不在群聊中"));
        memberRepository.delete(member);
        readCursorRepository.deleteByConversationIdAndUsername(conversationId, target);
        bumpVersion(conversation, LocalDateTime.now());
        return toDtoForUser(conversation, username);
    }

    @Transactional
    public void leave(String username, Long conversationId) {
        ChatConversation conversation = requireGroup(conversationId);
        ChatMember member = requireMember(conversationId, username);
        if (ChatConstants.ROLE_OWNER.equals(member.getRole())) {
            throw new BizException(400, "群主不能直接退群；请先转让或删除群聊");
        }
        memberRepository.delete(member);
        readCursorRepository.deleteByConversationIdAndUsername(conversationId, username);
        bumpVersion(conversation, LocalDateTime.now());
    }

    @Transactional
    public ConversationDtos.ConversationDto rename(String username,
                                                   Long conversationId,
                                                   String name) {
        ChatConversation conversation = requireGroup(conversationId);
        requireOwner(conversation, username);
        String newName = name == null ? "" : name.trim();
        if (newName.isEmpty() || newName.length() > 80) {
            throw new BizException(400, "群名称长度必须为 1-80 个字符");
        }
        LocalDateTime now = LocalDateTime.now();
        conversation.setName(newName);
        bumpVersion(conversation, now);
        return toDtoForUser(conversation, username);
    }

    /**
     * 更新当前设备的已读游标（单调不回退），并同步成员级最大游标。
     */
    @Transactional
    public ConversationDtos.ConversationDto markRead(String username,
                                                     Long conversationId,
                                                     String deviceKey,
                                                     long requestedSeq) {
        ChatConversation conversation = requireConversation(conversationId);
        ChatMember membership = requireMember(conversationId, username);
        ChatDevice device = deviceService.requireActiveDevice(username, deviceKey);

        long clamped = Math.max(0L, Math.min(requestedSeq, conversation.getLastMessageSeq()));
        LocalDateTime now = LocalDateTime.now();
        ChatReadCursor cursor = readCursorRepository
                .findByConversationIdAndDeviceKey(conversationId, device.getClientDeviceKey())
                .orElseGet(() -> {
                    ChatReadCursor c = new ChatReadCursor();
                    c.setConversationId(conversationId);
                    c.setUsername(username);
                    c.setDeviceId(device.getDeviceId());
                    c.setDeviceKey(device.getClientDeviceKey());
                    c.setLastReadSeq(0L);
                    return c;
                });
        long readSeq = Math.max(cursor.getLastReadSeq(), clamped);
        cursor.setLastReadSeq(readSeq);
        cursor.setUpdatedAt(now);
        readCursorRepository.save(cursor);

        if (membership.getLastReadSeq() == null || membership.getLastReadSeq() < readSeq) {
            membership.setLastReadSeq(readSeq);
            memberRepository.save(membership);
        }
        return toDto(conversation, memberRepository.findByConversationIdOrderByJoinedAtAsc(conversationId),
                membership.getRole(), readSeq);
    }

    /**
     * 多端同步状态：各会话版本号、最后消息序号、当前设备已读游标、待授权设备数。
     */
    @Transactional
    public ConversationDtos.SyncStateDto syncState(String username, String deviceKey) {
        ChatDevice device = deviceService.requireActiveDevice(username, deviceKey);
        List<ChatMember> memberships = memberRepository.findByUsernameOrderByJoinedAtAsc(username);
        List<Long> ids = memberships.stream().map(ChatMember::getConversationId).toList();
        Map<Long, ChatConversation> conversations = ids.isEmpty()
                ? Map.of()
                : conversationRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(ChatConversation::getId, Function.identity()));
        Map<Long, ChatReadCursor> cursors = readCursorRepository
                .findByUsernameAndDeviceKey(username, device.getClientDeviceKey())
                .stream()
                .collect(Collectors.toMap(ChatReadCursor::getConversationId, Function.identity(), (a, b) -> a));

        List<ConversationDtos.SyncConversationDto> result = new ArrayList<>();
        for (ChatMember membership : memberships) {
            ChatConversation conversation = conversations.get(membership.getConversationId());
            if (conversation == null) {
                continue;
            }
            long readSeq = resolveReadSeq(membership, cursors.get(conversation.getId()), true);
            result.add(new ConversationDtos.SyncConversationDto(
                    conversation.getId(),
                    conversation.getType(),
                    conversation.getName(),
                    conversation.getVersion(),
                    conversation.getLastMessageSeq(),
                    readSeq,
                    Math.max(0L, conversation.getLastMessageSeq() - readSeq),
                    conversation.getUpdatedAt()
            ));
        }
        result.sort(Comparator.comparing(ConversationDtos.SyncConversationDto::updatedAt, Comparator.reverseOrder()));

        long pendingLinks = deviceLinkRepository.countByUsernameAndStatus(username, ChatConstants.LINK_PENDING);
        return new ConversationDtos.SyncStateDto(
                device.getDeviceId(),
                device.getClientDeviceKey(),
                device.getDeviceName(),
                device.getStatus(),
                pendingLinks,
                result
        );
    }

    @Transactional
    public ChatMember requireMember(Long conversationId, String username) {
        return memberRepository.findByConversationIdAndUsername(conversationId, username)
                .orElseThrow(() -> new BizException(403, "你不是该会话成员"));
    }

    @Transactional
    public ChatConversation requireConversation(Long conversationId) {
        if (conversationId == null) {
            throw new BizException(400, "缺少 conversationId");
        }
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BizException(404, "会话不存在"));
    }

    private ChatConversation requireGroup(Long conversationId) {
        ChatConversation conversation = requireConversation(conversationId);
        if (!ChatConstants.CONVERSATION_GROUP.equals(conversation.getType())) {
            throw new BizException(400, "该操作仅支持群聊");
        }
        return conversation;
    }

    private void requireOwner(ChatConversation conversation, String username) {
        if (!username.equals(conversation.getOwnerUsername())) {
            throw new BizException(403, "只有群主可以执行该操作");
        }
    }

    private void bumpVersion(ChatConversation conversation, LocalDateTime now) {
        conversation.setVersion(conversation.getVersion() + 1);
        conversation.setUpdatedAt(now);
        conversationRepository.save(conversation);
    }

    private void addMember(Long conversationId, String username, String role, LocalDateTime now) {
        if (memberRepository.existsByConversationIdAndUsername(conversationId, username)) {
            return;
        }
        ChatMember member = new ChatMember();
        member.setConversationId(conversationId);
        member.setUsername(username);
        member.setRole(role);
        member.setJoinedAt(now);
        member.setLastReadSeq(0L);
        memberRepository.save(member);
    }

    private void ensureMember(Long conversationId, String username) {
        if (!memberRepository.existsByConversationIdAndUsername(conversationId, username)) {
            addMember(conversationId, username, ChatConstants.ROLE_MEMBER, LocalDateTime.now());
        }
    }

    private String requireOptionalActiveDevice(String username, String deviceKey) {
        if (deviceKey == null || deviceKey.isBlank()) {
            return null;
        }
        return deviceService.requireActiveDevice(username, deviceKey).getClientDeviceKey();
    }

    private long resolveReadSeq(ChatMember membership, ChatReadCursor cursor, boolean perDevice) {
        if (perDevice) {
            return cursor == null || cursor.getLastReadSeq() == null ? 0L : cursor.getLastReadSeq();
        }
        return membership.getLastReadSeq() == null ? 0L : membership.getLastReadSeq();
    }

    private String privateKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    private ConversationDtos.ConversationDto toDtoForUser(ChatConversation conversation, String username) {
        ChatMember me = memberRepository.findByConversationIdAndUsername(conversation.getId(), username)
                .orElse(null);
        long readSeq = me == null || me.getLastReadSeq() == null ? 0L : me.getLastReadSeq();
        return toDto(conversation,
                memberRepository.findByConversationIdOrderByJoinedAtAsc(conversation.getId()),
                me == null ? null : me.getRole(),
                readSeq);
    }

    private ConversationDtos.ConversationDto toDto(ChatConversation conversation,
                                                   List<ChatMember> members,
                                                   String myRole,
                                                   long readSeq) {
        List<ChatMember> safeMembers = members == null || members.isEmpty()
                ? memberRepository.findByConversationIdOrderByJoinedAtAsc(conversation.getId())
                : members;
        List<ConversationDtos.MemberDto> memberDtos = safeMembers.stream()
                .map(m -> new ConversationDtos.MemberDto(m.getUsername(), m.getRole(), m.getJoinedAt()))
                .toList();
        long lastSeq = conversation.getLastMessageSeq() == null ? 0L : conversation.getLastMessageSeq();
        return new ConversationDtos.ConversationDto(
                conversation.getId(),
                conversation.getType(),
                conversation.getName(),
                conversation.getOwnerUsername(),
                conversation.getVersion(),
                lastSeq,
                conversation.getLastMessageAt(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt(),
                myRole,
                readSeq,
                Math.max(0L, lastSeq - readSeq),
                memberDtos
        );
    }
}
