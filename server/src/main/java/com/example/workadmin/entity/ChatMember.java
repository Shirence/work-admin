package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 会话成员。role 为 OWNER / MEMBER。
 */
@Entity
@Table(name = "chat_member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_member", columnNames = {"conversation_id", "username"})
        },
        indexes = {
                @Index(name = "idx_chat_member_user", columnList = "username")
        })
public class ChatMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 10)
    private String role; // OWNER / MEMBER

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    /**
     * 账号级已读游标（该账号所有设备中的最大值）。
     * 多设备精确游标见 ChatReadCursor。
     */
    @Column(name = "last_read_seq", nullable = false)
    private Long lastReadSeq = 0L;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Long getLastReadSeq() {
        return lastReadSeq;
    }

    public void setLastReadSeq(Long lastReadSeq) {
        this.lastReadSeq = lastReadSeq;
    }
}
