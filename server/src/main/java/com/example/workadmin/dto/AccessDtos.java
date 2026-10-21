package com.example.workadmin.dto;

import java.util.List;

public final class AccessDtos {

    private AccessDtos() {
    }

    public record RoleDto(Long id, String name, String code, Boolean enabled) {
    }

    public record AccessResponse(RoleDto role, List<String> permissions, List<MenuDtos.MenuRow> menus) {
    }
}
