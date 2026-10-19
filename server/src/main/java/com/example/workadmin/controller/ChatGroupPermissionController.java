package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.chat.GroupPolicyDtos;
import com.example.workadmin.service.ChatGroupPolicyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 当前账号的建群权限（前端据此显示/隐藏“创建群聊”）。
 */
@RestController
@RequestMapping("/api/chat")
public class ChatGroupPermissionController {

    private final ChatGroupPolicyService groupPolicyService;

    public ChatGroupPermissionController(ChatGroupPolicyService groupPolicyService) {
        this.groupPolicyService = groupPolicyService;
    }

    /** GET /api/chat/group-permission */
    @GetMapping("/group-permission")
    public Result<GroupPolicyDtos.EffectivePermissionDto> effective(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok(groupPolicyService.effective(userInfo.getUsername()));
    }
}
