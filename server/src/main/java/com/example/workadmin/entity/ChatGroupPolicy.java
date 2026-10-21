package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 全局建群策略（单行表，id 固定为 1）。
 *
 * <ul>
 *   <li>AUTO_ALLOW：默认允许所有账号建群；</li>
 *   <li>AUTO_DENY：默认拒绝所有账号建群；</li>
 *   <li>MANUAL：默认拒绝，管理员给具体账号开 ALLOW 后才可建群。</li>
 * </ul>
 */
@Entity
@Table(name = "wa_chat_group_policy")
public class ChatGroupPolicy {

    @Id
    private Long id = 1L;

    @Column(nullable = false, length = 20)
    private String mode = "AUTO_ALLOW";

    @Column(name = "updated_by", length = 50)
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
