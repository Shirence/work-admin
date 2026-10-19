package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 逐设备密文信封。一条消息对每个目标设备生成一条 ChatEnvelope。
 *
 * <p>服务端只存密文，不解密、不检索正文。cipherType 对应 libsignal 的
 * WhisperMessage / PreKeyWhisperMessage 类型。</p>
 */
@Entity
@Table(name = "chat_envelope",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_envelope_device", columnNames = {"message_id", "target_device_key"})
        },
        indexes = {
                @Index(name = "idx_chat_envelope_target", columnList = "target_username,target_device_key,message_id"),
                @Index(name = "idx_chat_envelope_conversation", columnList = "conversation_id,message_id")
        })
public class ChatEnvelope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "target_username", nullable = false, length = 50)
    private String targetUsername;

    @Column(name = "target_device_id", nullable = false)
    private Integer targetDeviceId;

    @Column(name = "target_device_key", nullable = false, length = 80)
    private String targetDeviceKey;

    /** 2 = WhisperMessage，3 = PreKeyWhisperMessage（与 libsignal 保持一致）。 */
    @Column(name = "cipher_type", nullable = false)
    private Integer cipherType;

    /** Base64 编码的 libsignal 密文。 */
    @Lob
    @Column(nullable = false)
    private String body;

    /** X3DH 接收方在解密 PreKeyWhisperMessage 时需要，可为空。 */
    @Column(name = "registration_id")
    private Integer registrationId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public Integer getTargetDeviceId() {
        return targetDeviceId;
    }

    public void setTargetDeviceId(Integer targetDeviceId) {
        this.targetDeviceId = targetDeviceId;
    }

    public String getTargetDeviceKey() {
        return targetDeviceKey;
    }

    public void setTargetDeviceKey(String targetDeviceKey) {
        this.targetDeviceKey = targetDeviceKey;
    }

    public Integer getCipherType() {
        return cipherType;
    }

    public void setCipherType(Integer cipherType) {
        this.cipherType = cipherType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Integer getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Integer registrationId) {
        this.registrationId = registrationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
