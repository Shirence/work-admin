package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.chat.DeviceDtos;
import com.example.workadmin.service.ChatDeviceService;
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
 * 设备注册、授权、撤销与历史密钥授权。
 * 所有接口使用登录 token 里的 username 作为聊天身份。
 */
@RestController
@RequestMapping("/api/chat/devices")
public class ChatDeviceController {

    private final ChatDeviceService deviceService;

    public ChatDeviceController(ChatDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** POST /api/chat/devices/register */
    @PostMapping("/register")
    public Result<DeviceDtos.RegisterResult> register(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody DeviceDtos.DeviceRegisterRequest request) {
        DeviceDtos.RegisterResult result = deviceService.register(userInfo.getUsername(), request);
        if (result.pendingApproval()) {
            return Result.ok("设备已登记，等待已有设备授权", result);
        }
        return Result.ok("设备注册成功", result);
    }

    /** GET /api/chat/devices */
    @GetMapping
    public Result<List<DeviceDtos.DeviceDto>> list(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok(deviceService.listDevices(userInfo.getUsername()));
    }

    /** GET /api/chat/devices/pending?deviceKey=当前已激活设备 */
    @GetMapping("/pending")
    public Result<List<DeviceDtos.DeviceLinkDto>> pending(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam String deviceKey) {
        return Result.ok(deviceService.listPendingLinks(userInfo.getUsername(), deviceKey));
    }

    /** POST /api/chat/devices/approve */
    @PostMapping("/approve")
    public Result<DeviceDtos.DeviceDto> approve(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody DeviceDtos.ApproveRequest request) {
        return Result.ok("设备已授权",
                deviceService.approveDevice(userInfo.getUsername(),
                        request.approverDeviceKey(),
                        request.targetDeviceKey(),
                        request.grants()));
    }

    /** POST /api/chat/devices/reject */
    @PostMapping("/reject")
    public Result<DeviceDtos.DeviceDto> reject(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody DeviceDtos.RejectRequest request) {
        return Result.ok("已拒绝该设备",
                deviceService.reject(userInfo.getUsername(),
                        request.approverDeviceKey(),
                        request.targetDeviceKey()));
    }

    /** POST /api/chat/devices/revoke */
    @PostMapping("/revoke")
    public Result<DeviceDtos.DeviceDto> revoke(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody DeviceDtos.RevokeRequest request) {
        return Result.ok("设备已撤销",
                deviceService.revoke(userInfo.getUsername(),
                        request.actorDeviceKey(),
                        request.targetDeviceKey()));
    }

    /** GET /api/chat/devices/grants?deviceKey=新设备 */
    @GetMapping("/grants")
    public Result<List<DeviceDtos.GrantDto>> grants(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @RequestParam String deviceKey) {
        return Result.ok(deviceService.consumeGrants(userInfo.getUsername(), deviceKey));
    }

    /** POST /api/chat/devices/heartbeat */
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(
            @RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo,
            @Valid @RequestBody DeviceDtos.HeartbeatRequest request) {
        deviceService.heartbeat(userInfo.getUsername(), request.deviceKey());
        return Result.ok("ok", null);
    }
}
