package com.example.workadmin.dto;

public final class MenuDtos {

    private MenuDtos() {
    }

    public record MenuRow(Long id, Long parentId, String name, String type, String path,
                          String permission, Integer sortOrder, Boolean enabled, Boolean builtin) {
    }

    public record MenuRequest(Long parentId, String name, String type, String path,
                              String permission, Integer sortOrder, Boolean enabled) {
    }
}
