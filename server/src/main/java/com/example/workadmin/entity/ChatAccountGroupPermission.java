package com.example.workadmin.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 账号级建群权限覆盖。
 * overrideMode：INHERIT（继承全局）/ ALLOW（强制允许）/ DENY（强制拒绝）。
 * 只有管理员能修改。
 */
@Entity
@Table(name = "chat_account_group_permission",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_chat_group_perm_user", columnNames = {"username"})
        })
public class ChatAccountGroupPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "override_mode", nullable = false, length = 20)
    private String overrideMode = "INHERIT";

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getOverrideMode() {
        return overrideMode;
    }

    public void setOverrideMode(String overrideMode) {
        this.overrideMode = overrideMode;
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
