package com.example.workadmin.common;

import java.util.List;
import java.util.Map;

/**
 * 角色 -> 权限点。新注册的账号一律是 viewer（最小权限）。
 */
public final class RolePermissions {

    /** 新用户默认角色 */
    public static final String DEFAULT_ROLE = "viewer";

    private static final Map<String, List<String>> MAP = Map.of(
            "admin", List.of("user:add", "user:edit", "user:delete"),
            "manager", List.of("user:edit"),
            "viewer", List.of()
    );

    private RolePermissions() {
    }

    public static List<String> of(String role) {
        return MAP.getOrDefault(role, List.of());
    }

    public static boolean isValid(String role) {
        return MAP.containsKey(role);
    }
}
