package com.example.workadmin.dto;

public final class AccountDtos {

    private AccountDtos() {
    }

    /** 一行真实用户：姓名、账号来自 stu 的 `user` 表，角色来自 sys_role。 */
    public record AccountRow(Long id, String name, String account, Long roleId,
                             String roleName, String roleCode) {
    }

    public record AccountRequest(String name, String account, String password, Long roleId) {
    }

    public record RoleBindRequest(Long roleId) {
    }
}
