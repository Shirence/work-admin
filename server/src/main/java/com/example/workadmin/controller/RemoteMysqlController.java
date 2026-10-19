package com.example.workadmin.controller;

import com.example.workadmin.common.ChatAccess;
import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.remote.RemoteDtos;
import com.example.workadmin.service.RemoteMysqlService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 远程 stu 群聊库的只读观察台（仅管理员）。
 * 这是一个旁路查看功能，不参与 work-admin 的 H2 业务写入。
 */
@RestController
@RequestMapping("/api/remote")
public class RemoteMysqlController {

    private final RemoteMysqlService remoteMysqlService;

    public RemoteMysqlController(RemoteMysqlService remoteMysqlService) {
        this.remoteMysqlService = remoteMysqlService;
    }

    /** GET /api/remote/overview */
    @GetMapping("/overview")
    public Result<RemoteDtos.Overview> overview(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(remoteMysqlService.overview());
    }

    /** GET /api/remote/groups?keyword=&limit= */
    @GetMapping("/groups")
    public Result<List<RemoteDtos.GroupRow>> groups(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "200") Integer limit) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(remoteMysqlService.groups(keyword, limit));
    }

    /** GET /api/remote/groups/{id}  （群信息 + 成员） */
    @GetMapping("/groups/{id}")
    public Result<RemoteDtos.GroupDetail> group(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable long id) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(remoteMysqlService.group(id));
    }

    /** GET /api/remote/groups/{id}/messages?afterId=&limit= */
    @GetMapping("/groups/{id}/messages")
    public Result<RemoteDtos.MessagePage> groupMessages(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @PathVariable long id,
            @RequestParam(required = false, defaultValue = "0") Long afterId,
            @RequestParam(required = false, defaultValue = "200") Integer limit) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(remoteMysqlService.groupMessages(id, afterId, limit));
    }

    /** GET /api/remote/messages?keyword=&groupId=&limit=  （跨群最近消息） */
    @GetMapping("/messages")
    public Result<List<RemoteDtos.MessageRow>> messages(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false, defaultValue = "100") Integer limit) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(remoteMysqlService.recentMessages(keyword, groupId, limit));
    }

    /** GET /api/remote/users?keyword=&limit= */
    @GetMapping("/users")
    public Result<List<RemoteDtos.UserRow>> users(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "200") Integer limit) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(remoteMysqlService.users(keyword, limit));
    }
}
