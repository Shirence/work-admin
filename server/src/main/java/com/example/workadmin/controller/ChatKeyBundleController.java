package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.dto.chat.DeviceDtos;
import com.example.workadmin.service.ChatDeviceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 供发送方获取目标账号所有 ACTIVE 设备的 libsignal 密钥包。
 * 每次请求消费每台设备的一条一次性预共享密钥。
 */
@RestController
@RequestMapping("/api/chat/users")
public class ChatKeyBundleController {

    private final ChatDeviceService deviceService;

    public ChatKeyBundleController(ChatDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /** GET /api/chat/users/{username}/device-bundles */
    @GetMapping("/{username}/device-bundles")
    public Result<List<DeviceDtos.DeviceBundleDto>> bundles(@PathVariable String username) {
        return Result.ok(deviceService.bundlesFor(username));
    }
}
