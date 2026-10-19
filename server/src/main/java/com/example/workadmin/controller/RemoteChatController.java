package com.example.workadmin.controller;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.remote.RemoteChatDtos;
import com.example.workadmin.service.RemoteChatService;
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
 * 远程 stu 群聊：查看公开群、加入群聊、读历史、发消息。
 * 必须使用 /api/remote-auth/login 签发的远程 token。
 */
@RestController
@RequestMapping("/api/remote-chat")
public class RemoteChatController {

    private final RemoteChatService remoteChatService;

    public RemoteChatController(RemoteChatService remoteChatService) {
        this.remoteChatService = remoteChatService;
    }

    /** GET /api/remote-chat/groups?afterId=0&limit=200 */
    @GetMapping("/groups")
    public Result<List<RemoteChatDtos.GroupRow>> groups(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam(required = false, defaultValue = "0") Long afterId,
            @RequestParam(required = false, defaultValue = "200") Integer limit) {
        long userId = requireRemoteUserId(userInfo);
        return Result.ok(remoteChatService.groups(userId, afterId == null ? 0L : afterId, limit));
    }

    /** GET /api/remote-chat/groups/{id} */
    @GetMapping("/groups/{id}")
    public Result<RemoteChatDtos.GroupRow> group(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable long id) {
        long userId = requireRemoteUserId(userInfo);
        return Result.ok(remoteChatService.group(userId, id));
    }

    /** POST /api/remote-chat/groups  body: { name } */
    @PostMapping("/groups")
    public Result<RemoteChatDtos.GroupRow> createGroup(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody RemoteChatDtos.CreateGroupRequest request) {
        long userId = requireRemoteUserId(userInfo);
        return Result.ok("群聊创建成功", remoteChatService.createGroup(userId, request.name()));
    }

    /** POST /api/remote-chat/groups/{id}/join */
    @PostMapping("/groups/{id}/join")
    public Result<RemoteChatDtos.MemberRow> join(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable long id) {
        long userId = requireRemoteUserId(userInfo);
        return Result.ok("已加入群聊", remoteChatService.joinGroup(userId, id));
    }

    /** GET /api/remote-chat/groups/{id}/messages?afterId=0&limit=100 */
    @GetMapping("/groups/{id}/messages")
    public Result<RemoteChatDtos.MessagePage> messages(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable long id,
            @RequestParam(required = false, defaultValue = "0") Long afterId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        long userId = requireRemoteUserId(userInfo);
        return Result.ok(remoteChatService.history(
                userId, id, afterId == null ? 0L : afterId, limit));
    }

    /** POST /api/remote-chat/groups/{id}/messages  body: { content, clientMessageId } */
    @PostMapping("/groups/{id}/messages")
    public Result<RemoteChatDtos.MessageRow> send(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable long id,
            @Valid @RequestBody RemoteChatDtos.SendRequest request) {
        long userId = requireRemoteUserId(userInfo);
        return Result.ok("发送成功", remoteChatService.send(
                userId, id, request.content(), request.clientMessageId()));
    }

    private long requireRemoteUserId(UserInfo userInfo) {
        if (userInfo == null || !"REMOTE".equals(userInfo.getAuthSource()) || userInfo.getRemoteUserId() == null) {
            throw new BizException(403, "请先使用远程 stu 账号登录");
        }
        return userInfo.getRemoteUserId();
    }
}
