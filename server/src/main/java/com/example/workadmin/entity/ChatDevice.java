package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 聊天设备（多设备模型中的一台设备）。
 *
 * <p>服务端只保存公钥材料；身份私钥、签名私钥、一次性预共享密钥私钥永远留在设备本地。</p>
 *
 * <p>设备状态：</p>
 * <ul>
 *   <li>PENDING：新设备，等待同一账号下已有 ACTIVE 设备授权，不能收发/同步消息；</li>
 *   <li>ACTIVE：已激活，可获取密钥包、发送消息、同步消息；</li>
 *   <li>REVOKED：已撤销，不能继续使用。</li>
 * </ul>
 */
@Entity
@Table(name = "chat_device",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_device_client", columnNames = {"username", "client_device_key"}),
                @UniqueConstraint(name = "uk_chat_device_id", columnNames = {"username", "device_id"})
        },
        indexes = {
                @Index(name = "idx_chat_device_user_status", columnList = "username,status")
        })
public class ChatDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 聊天身份直接使用登录账号 username。 */
    @Column(nullable = false, length = 50)
    private String username;

    /** 客户端生成的稳定设备标识（建议 UUID），跨重装/重新注册保持不变。 */
    @Column(name = "client_device_key", nullable = false, length = 80)
    private String clientDeviceKey;

    /** 服务端在账号内分配的 libsignal deviceId（从 1 开始递增）。 */
    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "device_name", length = 120)
    private String deviceName;

    /** libsignal registrationId，范围 1..16383。 */
    @Column(name = "registration_id", nullable = false)
    private Integer registrationId;

    /** Base64 编码的 identity public key（33 字节，0x05 前缀 + 32 字节 Curve25519）。 */
    @Lob
    @Column(name = "identity_key", nullable = false)
    private String identityKey;

    @Column(name = "signed_pre_key_id", nullable = false)
    private Integer signedPreKeyId;

    /** Base64 编码的 signed pre-key public key。 */
    @Lob
    @Column(name = "signed_pre_key_public", nullable = false)
    private String signedPreKeyPublic;

    /** Base64 编码的 signed pre-key 签名（64 字节）。 */
    @Lob
    @Column(name = "signed_pre_key_signature", nullable = false)
    private String signedPreKeySignature;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "approved_by_device_key", length = 80)
    private String approvedByDeviceKey;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientDeviceKey() {
        return clientDeviceKey;
    }

    public void setClientDeviceKey(String clientDeviceKey) {
        this.clientDeviceKey = clientDeviceKey;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Integer registrationId) {
        this.registrationId = registrationId;
    }

    public String getIdentityKey() {
        return identityKey;
    }

    public void setIdentityKey(String identityKey) {
        this.identityKey = identityKey;
    }

    public Integer getSignedPreKeyId() {
        return signedPreKeyId;
    }

    public void setSignedPreKeyId(Integer signedPreKeyId) {
        this.signedPreKeyId = signedPreKeyId;
    }

    public String getSignedPreKeyPublic() {
        return signedPreKeyPublic;
    }

    public void setSignedPreKeyPublic(String signedPreKeyPublic) {
        this.signedPreKeyPublic = signedPreKeyPublic;
    }

    public String getSignedPreKeySignature() {
        return signedPreKeySignature;
    }

    public void setSignedPreKeySignature(String signedPreKeySignature) {
        this.signedPreKeySignature = signedPreKeySignature;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovedByDeviceKey() {
        return approvedByDeviceKey;
    }

    public void setApprovedByDeviceKey(String approvedByDeviceKey) {
        this.approvedByDeviceKey = approvedByDeviceKey;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(LocalDateTime lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
