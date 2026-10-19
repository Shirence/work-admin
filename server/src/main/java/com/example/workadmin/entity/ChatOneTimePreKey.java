package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 设备发布的一次性预共享公钥（X3DH 使用）。
 * 私钥只在设备本地保存；服务端在分发密钥包时原子地消费一条。
 */
@Entity
@Table(name = "chat_one_time_pre_key",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_otpk", columnNames = {"username", "device_id", "key_id"})
        },
        indexes = {
                @Index(name = "idx_chat_otpk_available", columnList = "username,device_id,used_flag")
        })
public class ChatOneTimePreKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "key_id", nullable = false)
    private Integer keyId;

    /** Base64 编码的 Curve25519 公钥（33 字节）。 */
    @Lob
    @Column(name = "public_key", nullable = false)
    private String publicKey;

    @Column(name = "used_flag", nullable = false)
    private Boolean used = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

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

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getKeyId() {
        return keyId;
    }

    public void setKeyId(Integer keyId) {
        this.keyId = keyId;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public Boolean getUsed() {
        return used;
    }

    public void setUsed(Boolean used) {
        this.used = used;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
