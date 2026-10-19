package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.chat.MessageDtos;
import com.example.workadmin.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 消息发送 / 增量同步。请求和响应中只有密文信封，没有明文正文。
 */
@RestController
@RequestMapping("/api/chat/messages")
public class ChatMessageController {

    private final ChatMessageService messageService;

    public ChatMessageController(ChatMessageService messageService) {
        this.messageService = messageService;
    }

    /** POST /api/chat/messages */
    @PostMapping
    public Result<MessageDtos.SendResult> send(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody MessageDtos.SendRequest request) {
        return Result.ok(messageService.send(userInfo.getUsername(), request));
    }

    /**
     * GET /api/chat/messages?conversationId=&deviceKey=&afterSeq=&limit=
     * 增量拉取 serverSeq > afterSeq 的消息。
     */
    @GetMapping
    public Result<List<MessageDtos.MessageDto>> sync(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam Long conversationId,
            @RequestParam String deviceKey,
            @RequestParam(required = false, defaultValue = "0") Long afterSeq,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        return Result.ok(messageService.sync(
                userInfo.getUsername(), conversationId, deviceKey, afterSeq, limit));
    }

    /**
     * GET /api/chat/messages/history?conversationId=&deviceKey=&limit=
     * 向更早翻页；新设备只有被授权后才能调用。
     */
    @GetMapping("/history")
    public Result<List<MessageDtos.MessageDto>> history(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam Long conversationId,
            @RequestParam String deviceKey,
            @RequestParam(required = false, defaultValue = "50") Integer limit) {
        return Result.ok(messageService.history(
                userInfo.getUsername(), conversationId, deviceKey, limit));
    }
}
