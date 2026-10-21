package com.example.workadmin.entity;


import java.time.LocalDateTime;

/**
 * 登录用户的值对象。
 * 数据来自 stu 项目的 `user` 表，这里只用于在内存里传递，不再映射成 JPA 实体。
 */
public class Account {

    private Long id;

    private String username;

    /** BCrypt 哈希值，长度 60 左右 */
    private String password;

    private String email;

    private String role;

    /** sys_role.id；旧数据为 null 时按 role 编码回填。 */
    private Long roleId;

    private Boolean enabled = true;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
