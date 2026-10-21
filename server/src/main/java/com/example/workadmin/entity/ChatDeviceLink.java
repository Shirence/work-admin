package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 新设备授权申请。
 *
 * <p>同一账号第二台及以后注册的设备处于 PENDING，并生成一条 link；
 * 已有 ACTIVE 设备确认后，link 置为 APPROVED，新设备才开始可同步消息。
 * 已有设备可在批准时附带若干“历史密钥授权”密文（ChatDeviceGrant），
 * 新设备解密后才能读取加入前的旧消息。</p>
 */
@Entity
@Table(name = "wa_chat_device_link",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_device_link", columnNames = {"username", "new_device_key"})
        },
        indexes = {
                @Index(name = "idx_chat_device_link_status", columnList = "username,status")
        })
public class ChatDeviceLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "new_device_id", nullable = false)
    private Integer newDeviceId;

    @Column(name = "new_device_key", nullable = false, length = 80)
    private String newDeviceKey;

    @Column(name = "new_device_name", length = 120)
    private String newDeviceName;

    /** PENDING / APPROVED / REJECTED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "decided_by_device_key", length = 80)
    private String decidedByDeviceKey;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

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

    public Integer getNewDeviceId() {
        return newDeviceId;
    }

    public void setNewDeviceId(Integer newDeviceId) {
        this.newDeviceId = newDeviceId;
    }

    public String getNewDeviceKey() {
        return newDeviceKey;
    }

    public void setNewDeviceKey(String newDeviceKey) {
        this.newDeviceKey = newDeviceKey;
    }

    public String getNewDeviceName() {
        return newDeviceName;
    }

    public void setNewDeviceName(String newDeviceName) {
        this.newDeviceName = newDeviceName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDecidedByDeviceKey() {
        return decidedByDeviceKey;
    }

    public void setDecidedByDeviceKey(String decidedByDeviceKey) {
        this.decidedByDeviceKey = decidedByDeviceKey;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }
}
