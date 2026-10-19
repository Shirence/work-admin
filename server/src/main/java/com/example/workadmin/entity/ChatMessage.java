package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 消息元数据。消息正文永远不在这里——正文只以密文形式写入 ChatEnvelope。
 *
 * <p>幂等键：(conversationId, senderUsername, senderClientDeviceKey, clientMessageId)。</p>
 * <p>定序键：(conversationId, serverSeq)，由服务端在会话行锁内单调分配。</p>
 */
@Entity
@Table(name = "chat_message",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_msg_client",
                        columnNames = {"conversation_id", "sender_username", "sender_client_device_key", "client_message_id"}),
                @UniqueConstraint(name = "uk_chat_msg_seq", columnNames = {"conversation_id", "server_seq"})
        },
        indexes = {
                @Index(name = "idx_chat_msg_conversation_seq", columnList = "conversation_id,server_seq")
        })
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_username", nullable = false, length = 50)
    private String senderUsername;

    @Column(name = "sender_device_id", nullable = false)
    private Integer senderDeviceId;

    @Column(name = "sender_client_device_key", nullable = false, length = 80)
    private String senderClientDeviceKey;

    /** 客户端生成的消息 ID，用于离线重发幂等去重。 */
    @Column(name = "client_message_id", nullable = false, length = 80)
    private String clientMessageId;

    @Column(name = "server_seq", nullable = false)
    private Long serverSeq;

    /** TEXT / IMAGE / FILE / SYSTEM ...（服务端不解析正文）。 */
    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType = "TEXT";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

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

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public Integer getSenderDeviceId() {
        return senderDeviceId;
    }

    public void setSenderDeviceId(Integer senderDeviceId) {
        this.senderDeviceId = senderDeviceId;
    }

    public String getSenderClientDeviceKey() {
        return senderClientDeviceKey;
    }

    public void setSenderClientDeviceKey(String senderClientDeviceKey) {
        this.senderClientDeviceKey = senderClientDeviceKey;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    public Long getServerSeq() {
        return serverSeq;
    }

    public void setServerSeq(Long serverSeq) {
        this.serverSeq = serverSeq;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
