package com.example.workadmin.common;

import com.example.workadmin.dto.UserInfo;

/**
 * 聊天模块的权限辅助方法。
 * 与现有系统一致：token 能证明“已登录”，管理员接口额外检查 role。
 */
public final class ChatAccess {

    private ChatAccess() {
    }

    public static void requireAdmin(UserInfo userInfo) {
        if (userInfo == null || !"admin".equalsIgnoreCase(userInfo.getRole())) {
            throw new BizException(403, "需要管理员权限");
        }
    }
}
