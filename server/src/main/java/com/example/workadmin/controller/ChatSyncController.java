package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.chat.ConversationDtos;
import com.example.workadmin.service.ChatConversationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多端同步状态。客户端先拉 state 比较 version/lastSeq，再按会话增量拉密文。
 */
@RestController
@RequestMapping("/api/chat/sync")
public class ChatSyncController {

    private final ChatConversationService conversationService;

    public ChatSyncController(ChatConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /** GET /api/chat/sync/state?deviceKey=... */
    @GetMapping("/state")
    public Result<ConversationDtos.SyncStateDto> state(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam String deviceKey) {
        return Result.ok(conversationService.syncState(userInfo.getUsername(), deviceKey));
    }
}
