package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.chat.ConversationDtos;
import com.example.workadmin.service.ChatConversationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 私聊 / 群聊 / 成员 / 已读游标。
 */
@RestController
@RequestMapping("/api/chat/conversations")
public class ChatConversationController {

    private final ChatConversationService conversationService;

    public ChatConversationController(ChatConversationService conversationService) {
        this.conversationService = conversationService;
    }

    /** POST /api/chat/conversations/private  body: { targetUsername } */
    @PostMapping("/private")
    public Result<ConversationDtos.ConversationDto> createPrivate(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody ConversationDtos.PrivateRequest request) {
        return Result.ok(conversationService.createPrivate(userInfo.getUsername(), request.targetUsername()));
    }

    /** POST /api/chat/conversations/group  body: { name, memberUsernames[] } */
    @PostMapping("/group")
    public Result<ConversationDtos.ConversationDto> createGroup(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody ConversationDtos.GroupRequest request) {
        return Result.ok("群聊创建成功",
                conversationService.createGroup(userInfo.getUsername(), request.name(), request.memberUsernames()));
    }

    /** GET /api/chat/conversations?deviceKey=... */
    @GetMapping
    public Result<List<ConversationDtos.ConversationDto>> list(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam(required = false) String deviceKey) {
        return Result.ok(conversationService.list(userInfo.getUsername(), deviceKey));
    }

    /** GET /api/chat/conversations/{id}?deviceKey=... */
    @GetMapping("/{id}")
    public Result<ConversationDtos.ConversationDto> get(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable Long id,
            @RequestParam(required = false) String deviceKey) {
        return Result.ok(conversationService.get(userInfo.getUsername(), id, deviceKey));
    }

    /** POST /api/chat/conversations/{id}/members  body: { usernames[] } */
    @PostMapping("/{id}/members")
    public Result<ConversationDtos.ConversationDto> addMembers(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable Long id,
            @Valid @RequestBody ConversationDtos.AddMembersRequest request) {
        return Result.ok(conversationService.addMembers(userInfo.getUsername(), id, request.usernames()));
    }

    /** POST /api/chat/conversations/{id}/members/remove  body: { username } */
    @PostMapping("/{id}/members/remove")
    public Result<ConversationDtos.ConversationDto> removeMember(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable Long id,
            @Valid @RequestBody ConversationDtos.RemoveMemberRequest request) {
        return Result.ok(conversationService.removeMember(userInfo.getUsername(), id, request.username()));
    }

    /** POST /api/chat/conversations/{id}/leave */
    @PostMapping("/{id}/leave")
    public Result<Void> leave(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable Long id) {
        conversationService.leave(userInfo.getUsername(), id);
        return Result.ok("已退出群聊", null);
    }

    /** POST /api/chat/conversations/{id}/rename  body: { name } */
    @PostMapping("/{id}/rename")
    public Result<ConversationDtos.ConversationDto> rename(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable Long id,
            @Valid @RequestBody ConversationDtos.RenameRequest request) {
        return Result.ok(conversationService.rename(userInfo.getUsername(), id, request.name()));
    }

    /** POST /api/chat/conversations/{id}/read  body: { deviceKey, lastReadSeq } */
    @PostMapping("/{id}/read")
    public Result<ConversationDtos.ConversationDto> markRead(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable Long id,
            @Valid @RequestBody ConversationDtos.ReadCursorRequest request) {
        return Result.ok(conversationService.markRead(
                userInfo.getUsername(), id, request.deviceKey(), request.lastReadSeq()));
    }
}
