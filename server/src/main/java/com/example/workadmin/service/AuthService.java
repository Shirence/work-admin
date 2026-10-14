package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.LoginRequest;
import com.example.workadmin.dto.LoginResponse;
import com.example.workadmin.dto.UserInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 登录与权限。演示账号与前端课程第 9 课保持一致：
 *   admin / manager / viewer，密码均为 123456。
 * 真实项目应把密码加密存数据库，并由服务端做接口鉴权。
 */
@Service
public class AuthService {

    private record Account(String password, String role, List<String> permissions) {
    }

    private static final Map<String, Account> ACCOUNTS = Map.of(
            "admin", new Account("123456", "admin", List.of("user:add", "user:edit", "user:delete")),
            "manager", new Account("123456", "manager", List.of("user:edit")),
            "viewer", new Account("123456", "viewer", List.of())
    );

    private final TokenStore tokenStore;

    public AuthService(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        Account account = ACCOUNTS.get(username);
        if (account == null || !account.password().equals(request.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        UserInfo userInfo = new UserInfo(username, account.role(), account.permissions());
        String token = tokenStore.create(userInfo);
        return new LoginResponse(token, userInfo);
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }
}
