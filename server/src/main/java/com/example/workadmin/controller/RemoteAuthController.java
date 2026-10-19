package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.LoginResponse;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.remote.RemoteChatDtos;
import com.example.workadmin.service.RemoteAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 远程 stu 账号登录。与本地 H2 账号体系相互独立。
 */
@RestController
@RequestMapping("/api/remote-auth")
public class RemoteAuthController {

    private final RemoteAuthService remoteAuthService;

    public RemoteAuthController(RemoteAuthService remoteAuthService) {
        this.remoteAuthService = remoteAuthService;
    }

    /** POST /api/remote-auth/login  body: { account, password } */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody RemoteChatDtos.LoginRequest request) {
        return Result.ok("远程账号登录成功", remoteAuthService.login(request));
    }

    /** POST /api/remote-auth/register  body: { name?, account, password, confirmPassword } */
    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RemoteChatDtos.RegisterRequest request) {
        return Result.ok("远程账号注册成功", remoteAuthService.register(request));
    }

    /** POST /api/remote-auth/logout */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "token", required = false) String token) {
        remoteAuthService.logout(token);
        return Result.ok("已退出远程登录", null);
    }

    /** GET /api/remote-auth/me */
    @GetMapping("/me")
    public Result<UserInfo> me(@RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok(userInfo);
    }
}
