package com.example.workadmin.controller;

import com.example.workadmin.common.RequirePermission;
import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.GroupChatDtos;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.service.GroupChatService;
import com.example.workadmin.service.StuStore;
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
 * 群聊接口。
 * <p>
 * 读写的是 stu 的 chat_group / chat_member / chat_message，消息明文存储，
 * 因此中台与 stu 前端看到的是同一批群组消息。
 */
@RestController
@RequestMapping("/api/groups")
@RequirePermission("chat:access")
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final StuStore store;

    public GroupChatController(GroupChatService groupChatService, StuStore store) {
        this.groupChatService = groupChatService;
        this.store = store;
    }

    /** 我参与的群（super_admin 返回全站群组）。 */
    @GetMapping
    public Result<List<GroupChatDtos.GroupRow>> groups(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok(groupChatService.groups(userInfo.getUsername()));
    }

    /** 建群时可选的人（不要求 user:read 权限，普通用户也能邀请别人）。 */
    @GetMapping("/candidates")
    public Result<List<GroupChatDtos.MemberRow>> candidates() {
        return Result.ok(store.listUsers(null).stream()
                .map(u -> new GroupChatDtos.MemberRow(u.id(), u.name(), u.account(), false, null))
                .toList());
    }

    @GetMapping("/{id}")
    public Result<GroupChatDtos.GroupDetail> detail(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok(groupChatService.detail(userInfo.getUsername(), id));
    }

    @GetMapping("/{id}/messages")
    public Result<List<GroupChatDtos.MessageRow>> messages(
            @PathVariable Long id,
            @RequestParam(value = "beforeId", required = false) Long beforeId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok(groupChatService.messages(userInfo.getUsername(), id, beforeId, limit));
    }

    @PostMapping("/{id}/messages")
    public Result<GroupChatDtos.MessageRow> send(
            @PathVariable Long id,
            @RequestBody GroupChatDtos.SendRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok("已发送", groupChatService.send(userInfo.getUsername(), id, request));
    }

    @PostMapping
    public Result<GroupChatDtos.GroupDetail> create(
            @RequestBody GroupChatDtos.CreateRequest request,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok("群已创建", groupChatService.create(userInfo.getUsername(), request));
    }

    @PostMapping("/{id}/join")
    public Result<GroupChatDtos.GroupDetail> join(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok("已加入群聊", groupChatService.join(userInfo.getUsername(), id));
    }
}
