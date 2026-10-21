package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.RolePermissions;
import com.example.workadmin.dto.LoginRequest;
import com.example.workadmin.dto.LoginResponse;
import com.example.workadmin.dto.RegisterRequest;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.entity.Account;
import com.example.workadmin.entity.Role;
import com.example.workadmin.repository.AccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录 / 注册。
 * 账号存在数据库 sys_account，密码为 BCrypt 哈希。
 */
@Service
public class AuthService {

    private final AccountRepository accountRepository;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final AccessService accessService;

    public AuthService(AccountRepository accountRepository,
                       TokenStore tokenStore,
                       PasswordEncoder passwordEncoder,
                       AccessService accessService) {
        this.accountRepository = accountRepository;
        this.tokenStore = tokenStore;
        this.passwordEncoder = passwordEncoder;
        this.accessService = accessService;
    }

    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        // 不区分「用户不存在」和「密码错误」，避免被枚举出有哪些账号
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(401, "账号或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new BizException(401, "账号或密码错误");
        }
        if (Boolean.FALSE.equals(account.getEnabled())) {
            throw new BizException(403, "账号已被禁用，请联系管理员");
        }
        UserInfo userInfo = userInfo(account);
        String token = tokenStore.create(userInfo);
        return new LoginResponse(token, userInfo);
    }

    @Transactional
    public UserInfo register(RegisterRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(400, "两次输入的密码不一致");
        }
        if (accountRepository.existsByUsername(username)) {
            throw new BizException(409, "用户名已被注册");
        }
        if (accountRepository.existsByEmail(email)) {
            throw new BizException(409, "邮箱已被注册");
        }

        Account account = new Account();
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setEmail(email);
        account.setRole(RolePermissions.DEFAULT_ROLE);
        account.setEnabled(true);
        account.setCreatedAt(LocalDateTime.now());
        accountRepository.save(account);
        Role role = accessService.roleFor(account);
        if (role != null) {
            account.setRoleId(role.getId());
            accountRepository.save(account);
        }

        return userInfo(account);
    }

    private UserInfo userInfo(Account account) {
        Role role = accessService.roleFor(account);
        String roleCode = role == null ? account.getRole() : role.getCode();
        String roleName = role == null ? account.getRole() : role.getName();
        return new UserInfo(account.getUsername(), roleCode,
                accessService.permissionsFor(account.getUsername()),
                role == null ? null : role.getId(), roleName,
                null, account.getUsername(), "LOCAL");
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }

    public UserInfo me(String username) {
        return userInfo(accessService.requireAccount(username));
    }
}
