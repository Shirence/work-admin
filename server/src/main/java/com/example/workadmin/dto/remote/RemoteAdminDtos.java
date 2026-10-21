package com.example.workadmin.dto.remote;

import java.util.List;

/**
 * 本地管理员管理远程 stu 账号的请求/响应。
 */
public final class RemoteAdminDtos {

    private RemoteAdminDtos() {
    }

    public record RemoteRoleRow(int id, String name, String code, boolean enabled) {
    }

    public record RemoteUserRow(int id, String name, String account, List<RemoteRoleRow> roles) {
    }

    public record RemoteUserRequest(String name, String account, String password, Integer roleId) {
    }

    public record RoleBindRequest(Integer roleId) {
    }
}
