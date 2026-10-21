package com.example.workadmin.common;

import com.example.workadmin.dto.UserInfo;

/**
 * 聊天模块的权限辅助方法。
 * token 能证明“已登录”，管理员接口额外检查角色：stu 体系里对应 super_admin。
 */
public final class ChatAccess {

    private ChatAccess() {
    }

    public static void requireAdmin(UserInfo userInfo) {
        if (userInfo == null || !isAdminRole(userInfo.getRole())) {
            throw new BizException(403, "需要管理员权限");
        }
    }

    /** super_admin 是 stu 体系里的最高角色；admin 保留兼容旧调用。 */
    private static boolean isAdminRole(String role) {
        return role != null && ("super_admin".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role));
    }
}
