package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 旧设备给新设备的“历史访问授权”密文。
 *
 * <p>服务端只保存不透明密文，例如被新设备身份公钥加密的群历史发送者密钥、
 * 或历史消息重加密包。新设备在设备被批准后拉取并消费。</p>
 */
@Entity
@Table(name = "chat_device_grant",
        indexes = {
                @Index(name = "idx_chat_device_grant_target", columnList = "to_device_key,consumed_flag"),
                @Index(name = "idx_chat_device_grant_link", columnList = "link_id")
        })
public class ChatDeviceGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "link_id", nullable = false)
    private Long linkId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "from_device_id", nullable = false)
    private Integer fromDeviceId;

    @Column(name = "from_device_key", nullable = false, length = 80)
    private String fromDeviceKey;

    @Column(name = "to_device_id", nullable = false)
    private Integer toDeviceId;

    @Column(name = "to_device_key", nullable = false, length = 80)
    private String toDeviceKey;

    /** 2 = WhisperMessage，3 = PreKeyWhisperMessage。 */
    @Column(name = "cipher_type", nullable = false)
    private Integer cipherType;

    /** Base64 编码的授权密文。 */
    @Lob
    @Column(name = "grant_body", nullable = false)
    private String body;

    @Column(name = "consumed_flag", nullable = false)
    private Boolean consumed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getFromDeviceId() {
        return fromDeviceId;
    }

    public void setFromDeviceId(Integer fromDeviceId) {
        this.fromDeviceId = fromDeviceId;
    }

    public String getFromDeviceKey() {
        return fromDeviceKey;
    }

    public void setFromDeviceKey(String fromDeviceKey) {
        this.fromDeviceKey = fromDeviceKey;
    }

    public Integer getToDeviceId() {
        return toDeviceId;
    }

    public void setToDeviceId(Integer toDeviceId) {
        this.toDeviceId = toDeviceId;
    }

    public String getToDeviceKey() {
        return toDeviceKey;
    }

    public void setToDeviceKey(String toDeviceKey) {
        this.toDeviceKey = toDeviceKey;
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

    public Boolean getConsumed() {
        return consumed;
    }

    public void setConsumed(Boolean consumed) {
        this.consumed = consumed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
