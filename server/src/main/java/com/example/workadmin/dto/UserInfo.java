package com.example.workadmin.dto;

import java.util.List;

/**
 * 登录后返回给前端的用户信息。
 *
 * <p>本地 H2 账号：authSource=LOCAL，remoteUserId/displayName 为空。
 * 远程 stu 账号：authSource=REMOTE，role=remote，remoteUserId 为远程 user.id。</p>
 */
public class UserInfo {

    private String username;
    private String role;
    private List<String> permissions;

    /** 远程 MySQL user.id；本地账号为 null。 */
    private Long remoteUserId;

    /** 远程账号昵称；本地账号为 username。 */
    private String displayName;

    /** LOCAL / REMOTE */
    private String authSource;

    public UserInfo() {
    }

    public UserInfo(String username, String role, List<String> permissions) {
        this(username, role, permissions, null, username, "LOCAL");
    }

    public UserInfo(String username,
                    String role,
                    List<String> permissions,
                    Long remoteUserId,
                    String displayName,
                    String authSource) {
        this.username = username;
        this.role = role;
        this.permissions = permissions;
        this.remoteUserId = remoteUserId;
        this.displayName = displayName;
        this.authSource = authSource;
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

    public Long getRemoteUserId() {
        return remoteUserId;
    }

    public void setRemoteUserId(Long remoteUserId) {
        this.remoteUserId = remoteUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAuthSource() {
        return authSource;
    }

    public void setAuthSource(String authSource) {
        this.authSource = authSource;
    }
}
