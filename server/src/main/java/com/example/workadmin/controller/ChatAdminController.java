package com.example.workadmin.controller;

import com.example.workadmin.common.ChatAccess;
import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.chat.DeviceDtos;
import com.example.workadmin.dto.chat.GroupPolicyDtos;
import com.example.workadmin.service.ChatDeviceService;
import com.example.workadmin.service.ChatGroupPolicyService;
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
 * 管理员：全局建群策略、账号级覆盖、设备重置。
 */
@RestController
@RequestMapping("/api/chat/admin")
public class ChatAdminController {

    private final ChatGroupPolicyService groupPolicyService;
    private final ChatDeviceService deviceService;

    public ChatAdminController(ChatGroupPolicyService groupPolicyService,
                               ChatDeviceService deviceService) {
        this.groupPolicyService = groupPolicyService;
        this.deviceService = deviceService;
    }

    /** GET /api/chat/admin/group-policy */
    @GetMapping("/group-policy")
    public Result<GroupPolicyDtos.PolicyDto> groupPolicy(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(groupPolicyService.getPolicy());
    }

    /** POST /api/chat/admin/group-policy  body: { mode } */
    @PostMapping("/group-policy")
    public Result<GroupPolicyDtos.PolicyDto> updateGroupPolicy(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody GroupPolicyDtos.PolicyUpdateRequest request) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok("建群策略已更新",
                groupPolicyService.updatePolicy(request.mode(), userInfo.getUsername()));
    }

    /** GET /api/chat/admin/account-permissions */
    @GetMapping("/account-permissions")
    public Result<List<GroupPolicyDtos.AccountPermissionDto>> accountPermissions(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(groupPolicyService.getPolicy().overrides());
    }

    /** POST /api/chat/admin/account-permissions  body: { username, overrideMode } */
    @PostMapping("/account-permissions")
    public Result<GroupPolicyDtos.AccountPermissionDto> updateAccountPermission(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody GroupPolicyDtos.PermissionUpdateRequest request) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok("账号建群权限已更新",
                groupPolicyService.setOverride(userInfo.getUsername(), request.username(), request.overrideMode()));
    }

    /** GET /api/chat/admin/devices?username=... */
    @GetMapping("/devices")
    public Result<List<DeviceDtos.DeviceDto>> devices(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam String username) {
        ChatAccess.requireAdmin(userInfo);
        return Result.ok(deviceService.listDevices(username));
    }

    /** POST /api/chat/admin/devices/reset  body: { username } */
    @PostMapping("/devices/reset")
    public Result<Void> resetDevices(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody DeviceDtos.AdminResetRequest request) {
        ChatAccess.requireAdmin(userInfo);
        deviceService.adminReset(request.username());
        return Result.ok("该账号设备已重置，下次注册将作为首台设备激活", null);
    }
}
