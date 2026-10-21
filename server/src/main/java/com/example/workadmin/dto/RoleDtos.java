package com.example.workadmin.dto;

public final class RoleDtos {

    private RoleDtos() {
    }

    public record RoleRow(Long id, String name, String code, String description,
                          Boolean enabled, Boolean builtin, long userCount) {
    }

    public record RoleRequest(String name, String code, String description, Boolean enabled) {
    }

    public record RoleOption(Long id, String name, String code) {
    }
}
