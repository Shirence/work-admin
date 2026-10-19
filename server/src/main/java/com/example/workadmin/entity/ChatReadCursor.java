package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 每设备已读游标。多设备各自上报已读位置，互不覆盖；
 * ChatMember.lastReadSeq 保存账号级最大值，便于未读数快速聚合。
 */
@Entity
@Table(name = "chat_read_cursor",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_read_cursor", columnNames = {"conversation_id", "device_key"})
        },
        indexes = {
                @Index(name = "idx_chat_read_cursor_user", columnList = "username,device_key")
        })
public class ChatReadCursor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "device_id", nullable = false)
    private Integer deviceId;

    @Column(name = "device_key", nullable = false, length = 80)
    private String deviceKey;

    @Column(name = "last_read_seq", nullable = false)
    private Long lastReadSeq = 0L;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
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

    public String getDeviceKey() {
        return deviceKey;
    }

    public void setDeviceKey(String deviceKey) {
        this.deviceKey = deviceKey;
    }

    public Long getLastReadSeq() {
        return lastReadSeq;
    }

    public void setLastReadSeq(Long lastReadSeq) {
        this.lastReadSeq = lastReadSeq;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
