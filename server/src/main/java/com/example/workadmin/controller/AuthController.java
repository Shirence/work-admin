package com.example.workadmin.controller;

import com.example.workadmin.common.Result;
import com.example.workadmin.config.AuthInterceptor;
import com.example.workadmin.dto.LoginRequest;
import com.example.workadmin.dto.LoginResponse;
import com.example.workadmin.dto.RegisterRequest;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** POST /api/login  body: { username, password } */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok("登录成功", authService.login(request));
    }

    /** POST /api/register  body: { username, password, confirmPassword, email } */
    @PostMapping("/register")
    public Result<UserInfo> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok("注册成功", authService.register(request));
    }

    /** POST /api/logout */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "token", required = false) String token) {
        authService.logout(token);
        return Result.ok("已退出登录", null);
    }

    /** GET /api/me */
    @GetMapping("/me")
    public Result<UserInfo> me(@RequestAttribute(AuthInterceptor.CURRENT_USER) UserInfo userInfo) {
        return Result.ok(userInfo);
    }

    /** GET /api/health  不需要 token */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("UP", null);
    }
}
