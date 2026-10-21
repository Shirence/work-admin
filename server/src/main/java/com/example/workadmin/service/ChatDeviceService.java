package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.ChatConstants;
import com.example.workadmin.dto.chat.DeviceDtos;
import com.example.workadmin.entity.ChatDevice;
import com.example.workadmin.entity.ChatDeviceGrant;
import com.example.workadmin.entity.ChatDeviceLink;
import com.example.workadmin.entity.ChatOneTimePreKey;
import com.example.workadmin.repository.ChatDeviceGrantRepository;
import com.example.workadmin.repository.ChatDeviceLinkRepository;
import com.example.workadmin.repository.ChatDeviceRepository;
import com.example.workadmin.repository.ChatOneTimePreKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 多设备与设备密钥。
 *
 * <p>安全边界：服务端只接收公钥材料（identity key / signed pre-key / one-time pre-key），
 * 私钥永远不离开客户端。新设备处于 PENDING，必须由同账号下已有 ACTIVE 设备批准后才能
 * 拉取消息；批准时可以附带只对新设备可解的历史密钥授权密文。</p>
 */
@Service
public class ChatDeviceService {

    private final ChatDeviceRepository deviceRepository;
    private final ChatOneTimePreKeyRepository preKeyRepository;
    private final ChatDeviceLinkRepository linkRepository;
    private final ChatDeviceGrantRepository grantRepository;
    private final StuStore store;

    public ChatDeviceService(ChatDeviceRepository deviceRepository,
                             ChatOneTimePreKeyRepository preKeyRepository,
                             ChatDeviceLinkRepository linkRepository,
                             ChatDeviceGrantRepository grantRepository,
                             StuStore store) {
        this.deviceRepository = deviceRepository;
        this.preKeyRepository = preKeyRepository;
        this.linkRepository = linkRepository;
        this.grantRepository = grantRepository;
        this.store = store;
    }

    /**
     * 注册或更新一台设备。
     * <p>账号的第一台设备自动 ACTIVE；之后的设备 PENDING，等待已有设备授权。</p>
     */
    @Transactional
    public DeviceDtos.RegisterResult register(String username, DeviceDtos.DeviceRegisterRequest request) {
        String clientDeviceKey = request.clientDeviceKey().trim();
        validateKeyMaterial(request);

        List<ChatDevice> devices = deviceRepository.findByUsernameForUpdate(username);
        LocalDateTime now = LocalDateTime.now();

        ChatDevice existing = devices.stream()
                .filter(d -> d.getClientDeviceKey().equals(clientDeviceKey))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            if (ChatConstants.DEVICE_REVOKED.equals(existing.getStatus())) {
                throw new BizException(403, "该设备已被撤销，不能重新注册；请使用新的设备标识");
            }
            applyKeyMaterial(existing, request, now);
            deviceRepository.save(existing);
            storeOneTimePreKeys(username, existing.getDeviceId(), request.oneTimePreKeys());
            ChatDeviceLink link = linkRepository.findByUsernameAndNewDeviceKey(username, clientDeviceKey).orElse(null);
            return new DeviceDtos.RegisterResult(
                    toDto(existing),
                    ChatConstants.DEVICE_PENDING.equals(existing.getStatus()),
                    link == null ? null : link.getId()
            );
        }

        boolean firstDevice = devices.isEmpty();
        int nextDeviceId = devices.stream()
                .map(ChatDevice::getDeviceId)
                .filter(id -> id != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        ChatDevice device = new ChatDevice();
        device.setUsername(username);
        device.setClientDeviceKey(clientDeviceKey);
        device.setDeviceId(nextDeviceId);
        device.setCreatedAt(now);
        device.setLastSeenAt(now);
        device.setStatus(firstDevice ? ChatConstants.DEVICE_ACTIVE : ChatConstants.DEVICE_PENDING);
        if (firstDevice) {
            device.setApprovedByDeviceKey(clientDeviceKey);
            device.setApprovedAt(now);
        }
        applyKeyMaterial(device, request, now);
        deviceRepository.save(device);
        storeOneTimePreKeys(username, device.getDeviceId(), request.oneTimePreKeys());

        Long linkId = null;
        if (!firstDevice) {
            ChatDeviceLink link = new ChatDeviceLink();
            link.setUsername(username);
            link.setNewDeviceId(device.getDeviceId());
            link.setNewDeviceKey(clientDeviceKey);
            link.setNewDeviceName(device.getDeviceName());
            link.setStatus(ChatConstants.LINK_PENDING);
            link.setRequestedAt(now);
            linkId = linkRepository.save(link).getId();
        }

        return new DeviceDtos.RegisterResult(toDto(device), !firstDevice, linkId);
    }

    @Transactional
    public List<DeviceDtos.DeviceDto> listDevices(String username) {
        return deviceRepository.findByUsernameOrderByDeviceIdAsc(username)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<DeviceDtos.DeviceLinkDto> listPendingLinks(String username, String approverDeviceKey) {
        requireActiveDevice(username, approverDeviceKey);
        return linkRepository.findByUsernameAndStatusOrderByRequestedAtAsc(username, ChatConstants.LINK_PENDING)
                .stream()
                .map(this::toLinkDto)
                .toList();
    }

    /**
     * 批准指定新设备。
     */
    @Transactional
    public DeviceDtos.DeviceDto approveDevice(String username,
                                              String approverDeviceKey,
                                              String targetDeviceKey,
                                              List<DeviceDtos.GrantPayload> grants) {
        ChatDevice approver = requireActiveDevice(username, approverDeviceKey);
        if (approver.getClientDeviceKey().equals(targetDeviceKey)) {
            throw new BizException(400, "不能授权当前设备自己");
        }
        ChatDevice target = deviceRepository
                .findByUsernameAndClientDeviceKey(username, targetDeviceKey)
                .orElseThrow(() -> new BizException(404, "待授权设备不存在"));
        if (ChatConstants.DEVICE_REVOKED.equals(target.getStatus())) {
            throw new BizException(409, "设备已被撤销，不能授权");
        }
        ChatDeviceLink link = linkRepository
                .findByUsernameAndNewDeviceKey(username, targetDeviceKey)
                .orElseThrow(() -> new BizException(404, "设备授权申请不存在"));

        LocalDateTime now = LocalDateTime.now();
        if (!ChatConstants.DEVICE_ACTIVE.equals(target.getStatus())) {
            target.setStatus(ChatConstants.DEVICE_ACTIVE);
            target.setApprovedByDeviceKey(approverDeviceKey);
            target.setApprovedAt(now);
            deviceRepository.save(target);

            link.setStatus(ChatConstants.LINK_APPROVED);
            link.setDecidedByDeviceKey(approverDeviceKey);
            link.setDecidedAt(now);
            linkRepository.save(link);

            if (grants != null && !grants.isEmpty()) {
                List<ChatDeviceGrant> rows = new ArrayList<>();
                for (DeviceDtos.GrantPayload grant : grants) {
                    validateGrant(grant);
                    ChatDeviceGrant row = new ChatDeviceGrant();
                    row.setLinkId(link.getId());
                    row.setUsername(username);
                    row.setFromDeviceId(approver.getDeviceId());
                    row.setFromDeviceKey(approver.getClientDeviceKey());
                    row.setToDeviceId(target.getDeviceId());
                    row.setToDeviceKey(target.getClientDeviceKey());
                    row.setCipherType(grant.cipherType());
                    row.setBody(grant.body().trim());
                    row.setConsumed(false);
                    row.setCreatedAt(now);
                    rows.add(row);
                }
                grantRepository.saveAll(rows);
            }
        }
        return toDto(target);
    }

    @Transactional
    public DeviceDtos.DeviceDto reject(String username, String approverDeviceKey, String targetDeviceKey) {
        requireActiveDevice(username, approverDeviceKey);
        ChatDevice target = deviceRepository
                .findByUsernameAndClientDeviceKey(username, targetDeviceKey)
                .orElseThrow(() -> new BizException(404, "待授权设备不存在"));
        if (ChatConstants.DEVICE_REVOKED.equals(target.getStatus())) {
            return toDto(target);
        }
        LocalDateTime now = LocalDateTime.now();
        target.setStatus(ChatConstants.DEVICE_REVOKED);
        deviceRepository.save(target);
        preKeyRepository.deleteByUsernameAndDeviceId(username, target.getDeviceId());

        linkRepository.findByUsernameAndNewDeviceKey(username, targetDeviceKey).ifPresent(link -> {
            link.setStatus(ChatConstants.LINK_REJECTED);
            link.setDecidedByDeviceKey(approverDeviceKey);
            link.setDecidedAt(now);
            linkRepository.save(link);
        });
        return toDto(target);
    }

    @Transactional
    public DeviceDtos.DeviceDto revoke(String username, String actorDeviceKey, String targetDeviceKey) {
        ChatDevice actor = requireActiveDevice(username, actorDeviceKey);
        if (actor.getClientDeviceKey().equals(targetDeviceKey)) {
            throw new BizException(400, "不能撤销当前正在使用的设备，请在另一台已激活设备上操作");
        }
        ChatDevice target = deviceRepository
                .findByUsernameAndClientDeviceKey(username, targetDeviceKey)
                .orElseThrow(() -> new BizException(404, "设备不存在"));
        if (ChatConstants.DEVICE_REVOKED.equals(target.getStatus())) {
            return toDto(target);
        }
        if (ChatConstants.DEVICE_ACTIVE.equals(target.getStatus())
                && deviceRepository.countByUsernameAndStatus(username, ChatConstants.DEVICE_ACTIVE) <= 1) {
            throw new BizException(409, "至少需要保留一台已激活设备");
        }
        LocalDateTime now = LocalDateTime.now();
        target.setStatus(ChatConstants.DEVICE_REVOKED);
        deviceRepository.save(target);
        preKeyRepository.deleteByUsernameAndDeviceId(username, target.getDeviceId());
        grantRepository.deleteByToDeviceKey(targetDeviceKey);
        linkRepository.findByUsernameAndNewDeviceKey(username, targetDeviceKey).ifPresent(link -> {
            if (ChatConstants.LINK_PENDING.equals(link.getStatus())) {
                link.setStatus(ChatConstants.LINK_REJECTED);
                link.setDecidedByDeviceKey(actorDeviceKey);
                link.setDecidedAt(now);
                linkRepository.save(link);
            }
        });
        return toDto(target);
    }

    /**
     * 新设备激活后拉取历史访问授权密文；拉取即消费。
     */
    @Transactional
    public List<DeviceDtos.GrantDto> consumeGrants(String username, String deviceKey) {
        ChatDevice device = requireActiveDevice(username, deviceKey);
        List<ChatDeviceGrant> grants = grantRepository
                .findByUsernameAndToDeviceKeyAndConsumedFalseOrderByCreatedAtAsc(username, device.getClientDeviceKey());
        LocalDateTime now = LocalDateTime.now();
        List<DeviceDtos.GrantDto> result = new ArrayList<>();
        for (ChatDeviceGrant grant : grants) {
            grant.setConsumed(true);
            result.add(new DeviceDtos.GrantDto(
                    grant.getId(),
                    grant.getLinkId(),
                    grant.getFromDeviceKey(),
                    grant.getCipherType(),
                    grant.getBody(),
                    grant.getCreatedAt()
            ));
        }
        grantRepository.saveAll(grants);
        return result;
    }

    @Transactional
    public void heartbeat(String username, String deviceKey) {
        ChatDevice device = deviceRepository
                .findByUsernameAndClientDeviceKey(username, deviceKey)
                .orElseThrow(() -> new BizException(404, "设备不存在"));
        device.setLastSeenAt(LocalDateTime.now());
        deviceRepository.save(device);
    }

    /**
     * 给发送方返回某账号所有 ACTIVE 设备的 libsignal 密钥包。
     * 每台设备的一次性预共享密钥只消费一条。
     */
    @Transactional
    public List<DeviceDtos.DeviceBundleDto> bundlesFor(String username) {
        if (!store.accountExists(username)) {
            throw new BizException(404, "账号不存在：" + username);
        }
        List<ChatDevice> devices = deviceRepository
                .findByUsernameAndStatusOrderByDeviceIdAsc(username, ChatConstants.DEVICE_ACTIVE);
        List<DeviceDtos.DeviceBundleDto> bundles = new ArrayList<>(devices.size());
        for (ChatDevice device : devices) {
            ChatOneTimePreKey preKey = consumeOneTimePreKey(username, device.getDeviceId());
            bundles.add(new DeviceDtos.DeviceBundleDto(
                    username,
                    device.getDeviceId(),
                    device.getClientDeviceKey(),
                    device.getRegistrationId(),
                    device.getIdentityKey(),
                    device.getSignedPreKeyId(),
                    device.getSignedPreKeyPublic(),
                    device.getSignedPreKeySignature(),
                    preKey == null ? null : new DeviceDtos.OneTimePreKeyPublicDto(preKey.getKeyId(), preKey.getPublicKey())
            ));
        }
        return bundles;
    }

    /**
     * 校验设备属于该账号且处于 ACTIVE；否则拒绝发送/同步/已读操作。
     */
    @Transactional
    public ChatDevice requireActiveDevice(String username, String deviceKey) {
        String key = deviceKey == null ? "" : deviceKey.trim();
        if (key.isEmpty()) {
            throw new BizException(400, "缺少设备标识 deviceKey");
        }
        ChatDevice device = deviceRepository
                .findByUsernameAndClientDeviceKey(username, key)
                .orElseThrow(() -> new BizException(404, "设备不存在，请先注册设备"));
        if (!ChatConstants.DEVICE_ACTIVE.equals(device.getStatus())) {
            throw new BizException(403, "设备未激活，请先用同一账号下已有设备完成授权");
        }
        return device;
    }

    /**
     * 管理员重置某账号的所有设备（用于设备全部丢失/撤销后恢复）。
     * 下一次注册的设备会重新成为第一台 ACTIVE 设备。
     */
    @Transactional
    public void adminReset(String username) {
        if (!store.accountExists(username)) {
            throw new BizException(404, "账号不存在：" + username);
        }
        grantRepository.deleteByUsername(username);
        linkRepository.deleteByUsername(username);
        preKeyRepository.deleteByUsername(username);
        deviceRepository.deleteAllInBatch(deviceRepository.findByUsernameOrderByDeviceIdAsc(username));
    }

    @Transactional
    public DeviceDtos.DeviceLinkDto toLink(String username, String deviceKey) {
        ChatDeviceLink link = linkRepository.findByUsernameAndNewDeviceKey(username, deviceKey)
                .orElseThrow(() -> new BizException(404, "设备授权申请不存在"));
        return toLinkDto(link);
    }

    private ChatOneTimePreKey consumeOneTimePreKey(String username, Integer deviceId) {
        List<ChatOneTimePreKey> keys = preKeyRepository.findUnusedForUpdate(username, deviceId);
        if (keys.isEmpty()) {
            return null;
        }
        ChatOneTimePreKey key = keys.get(0);
        key.setUsed(true);
        preKeyRepository.save(key);
        return key;
    }

    private void applyKeyMaterial(ChatDevice device,
                                  DeviceDtos.DeviceRegisterRequest request,
                                  LocalDateTime now) {
        device.setDeviceName(request.deviceName() == null ? null : request.deviceName().trim());
        device.setRegistrationId(request.registrationId());
        device.setIdentityKey(request.identityKey().trim());
        device.setSignedPreKeyId(request.signedPreKeyId());
        device.setSignedPreKeyPublic(request.signedPreKeyPublic().trim());
        device.setSignedPreKeySignature(request.signedPreKeySignature().trim());
        device.setLastSeenAt(now);
    }

    private void storeOneTimePreKeys(String username,
                                     Integer deviceId,
                                     List<DeviceDtos.OneTimePreKeyDto> keys) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        if (keys.size() > ChatConstants.MAX_ONE_TIME_PRE_KEYS) {
            throw new BizException(400, "一次性预共享密钥数量不能超过 " + ChatConstants.MAX_ONE_TIME_PRE_KEYS);
        }
        Set<Integer> ids = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (DeviceDtos.OneTimePreKeyDto dto : keys) {
            if (!ids.add(dto.keyId())) {
                throw new BizException(400, "一次性预共享密钥 keyId 重复：" + dto.keyId());
            }
            if (preKeyRepository.findByUsernameAndDeviceIdAndKeyId(username, deviceId, dto.keyId()).isPresent()) {
                continue;
            }
            ChatOneTimePreKey key = new ChatOneTimePreKey();
            key.setUsername(username);
            key.setDeviceId(deviceId);
            key.setKeyId(dto.keyId());
            key.setPublicKey(dto.publicKey().trim());
            key.setUsed(false);
            key.setCreatedAt(now);
            preKeyRepository.save(key);
        }
    }

    private void validateKeyMaterial(DeviceDtos.DeviceRegisterRequest request) {
        requirePublicKey(request.identityKey(), "identityKey");
        requirePublicKey(request.signedPreKeyPublic(), "signedPreKeyPublic");
        requireSignature(request.signedPreKeySignature(), "signedPreKeySignature");
        if (request.oneTimePreKeys() == null) {
            return;
        }
        if (request.oneTimePreKeys().size() > ChatConstants.MAX_ONE_TIME_PRE_KEYS) {
            throw new BizException(400, "一次性预共享密钥数量不能超过 " + ChatConstants.MAX_ONE_TIME_PRE_KEYS);
        }
        Set<Integer> ids = new HashSet<>();
        for (DeviceDtos.OneTimePreKeyDto dto : request.oneTimePreKeys()) {
            if (!ids.add(dto.keyId())) {
                throw new BizException(400, "一次性预共享密钥 keyId 重复：" + dto.keyId());
            }
            requirePublicKey(dto.publicKey(), "oneTimePreKeys[" + dto.keyId() + "].publicKey");
        }
    }

    private void validateGrant(DeviceDtos.GrantPayload grant) {
        if (grant.cipherType() < 1 || grant.cipherType() > 3) {
            throw new BizException(400, "历史授权 cipherType 不合法");
        }
        decodeBase64(grant.body(), "历史授权 body");
    }

    private void requirePublicKey(String value, String field) {
        int length = decodeBase64(value, field).length;
        if (length != 32 && length != 33) {
            throw new BizException(400, field + " 必须是 32/33 字节的 Curve25519 公钥");
        }
    }

    private void requireSignature(String value, String field) {
        int length = decodeBase64(value, field).length;
        if (length != 64) {
            throw new BizException(400, field + " 必须是 64 字节签名");
        }
    }

    private byte[] decodeBase64(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new BizException(400, field + " 不能为空");
        }
        try {
            return Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException e) {
            throw new BizException(400, field + " 不是合法 Base64");
        }
    }

    private DeviceDtos.DeviceDto toDto(ChatDevice device) {
        return new DeviceDtos.DeviceDto(
                device.getId(),
                device.getDeviceId(),
                device.getClientDeviceKey(),
                device.getDeviceName(),
                device.getStatus(),
                device.getRegistrationId(),
                device.getApprovedByDeviceKey(),
                device.getApprovedAt(),
                device.getCreatedAt(),
                device.getLastSeenAt()
        );
    }

    private DeviceDtos.DeviceLinkDto toLinkDto(ChatDeviceLink link) {
        return new DeviceDtos.DeviceLinkDto(
                link.getId(),
                link.getNewDeviceId(),
                link.getNewDeviceKey(),
                link.getNewDeviceName(),
                link.getStatus(),
                link.getRequestedAt(),
                link.getDecidedAt()
        );
    }
}
