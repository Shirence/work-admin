package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 会话：一对一（PRIVATE）或群聊（GROUP）。
 *
 * <p>私聊通过排序后的两个 username 生成唯一 privateKey，保证同一对账号只有一条会话。</p>
 * <p>version 是“会话/群版本号”，只在元数据或成员变化时自增；消息顺序由每条消息的
 * serverSeq 表示，两者互不替代。</p>
 */
@Entity
@Table(name = "wa_chat_conversation",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_conversation_private", columnNames = {"private_key"})
        },
        indexes = {
                @Index(name = "idx_chat_conversation_updated", columnList = "updated_at")
        })
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PRIVATE / GROUP */
    @Column(nullable = false, length = 10)
    private String type;

    @Column(length = 80)
    private String name;

    @Column(name = "owner_username", length = 50)
    private String ownerUsername;

    /** 仅私聊使用：minUsername + "|" + maxUsername。 */
    @Column(name = "private_key", unique = true, length = 120)
    private String privateKey;

    /** 群版本号：成员/名称变化时 +1。 */
    @Column(nullable = false)
    private Long version = 1L;

    /** 会话内最后一条消息的 serverSeq，用于分配下一个序号。 */
    @Column(name = "last_message_seq", nullable = false)
    private Long lastMessageSeq = 0L;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getLastMessageSeq() {
        return lastMessageSeq;
    }

    public void setLastMessageSeq(Long lastMessageSeq) {
        this.lastMessageSeq = lastMessageSeq;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
