package com.example.workadmin.dto;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record AccountRow(Long id, String username, String email, Long roleId,
                             String roleName, String roleCode, Boolean enabled) {
    }

    public record AccountRequest(String username, String email, String password,
                                 Long roleId, Boolean enabled) {
    }

    public record RoleBindRequest(Long roleId) {
    }
}
