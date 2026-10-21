package com.example.workadmin.dto;

import java.util.List;

/**
 * 登录后返回给前端的用户信息。
 */
public class UserInfo {

    private String username;
    private String role;
    private List<String> permissions;

    /** sys_role.id。 */
    private Long roleId;

    /** 角色显示名。 */
    private String roleName;

    public UserInfo() {
    }

    public UserInfo(String username, String role, List<String> permissions) {
        this.username = username;
        this.role = role;
        this.permissions = permissions;
    }

    public UserInfo(String username, String role, List<String> permissions, Long roleId, String roleName) {
        this.username = username;
        this.role = role;
        this.permissions = permissions;
        this.roleId = roleId;
        this.roleName = roleName;
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

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
}
