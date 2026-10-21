package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.StuPassword;
import com.example.workadmin.dto.LoginRequest;
import com.example.workadmin.dto.LoginResponse;
import com.example.workadmin.dto.RegisterRequest;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.entity.Account;
import com.example.workadmin.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 登录 / 注册。
 * <p>
 * 中台与 stu 共用账号：身份取自 `user` 表（account + passwd），
 * 密码算法与 stu 完全一致（三重 MD5，兼容历史单次 MD5）。
 * 注册即向 `user` 表写入一行，并绑定 member 角色。
 */
@Service
public class AuthService {

    private static final String DEFAULT_ROLE = "member";

    private final StuStore store;
    private final AccessService accessService;
    private final TokenStore tokenStore;

    public AuthService(StuStore store, AccessService accessService, TokenStore tokenStore) {
        this.store = store;
        this.accessService = accessService;
        this.tokenStore = tokenStore;
    }

    public LoginResponse login(LoginRequest request) {
        String account = request.getUsername() == null ? "" : request.getUsername().trim();
        // 不区分「账号不存在」和「密码错误」，避免被枚举
        StuStore.StuUser user = store.findUserByAccount(account)
                .orElseThrow(() -> new BizException(401, "账号或密码错误"));
        if (!StuPassword.matches(request.getPassword(), user.passwd())) {
            throw new BizException(401, "账号或密码错误");
        }
        UserInfo userInfo = userInfo(accessService.requireAccount(user.account()));
        String token = tokenStore.create(userInfo);
        return new LoginResponse(token, userInfo);
    }

    @Transactional
    public UserInfo register(RegisterRequest request) {
        String account = request.getUsername() == null ? "" : request.getUsername().trim();
        String name = request.getName() == null || request.getName().isBlank()
                ? account : request.getName().trim();

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BizException(400, "两次输入的密码不一致");
        }
        if (store.accountExists(account)) {
            throw new BizException(409, "账号已被注册");
        }
        Role role = store.findRoleByCode(DEFAULT_ROLE)
                .map(r -> {
                    Role value = new Role();
                    value.setId(r.id());
                    value.setName(r.name());
                    value.setCode(r.code());
                    value.setEnabled(r.enabled());
                    return value;
                })
                .orElseThrow(() -> new BizException(500, "默认角色 member 不存在"));
        if (Boolean.FALSE.equals(role.getEnabled())) {
            throw new BizException(400, "默认角色已停用，暂时无法注册");
        }

        long id = store.insertUser(name, account, StuPassword.hash(request.getPassword()), role.getId());
        return userInfo(accessService.requireAccount(accountOrThrow(id, account)));
    }

    private String accountOrThrow(long id, String account) {
        return store.findUserById(id).map(StuStore.StuUser::account).orElse(account);
    }

    private UserInfo userInfo(Account account) {
        Role role = accessService.roleFor(account);
        return new UserInfo(account.getUsername(),
                role == null ? account.getRole() : role.getCode(),
                accessService.permissionsFor(account.getUsername()),
                role == null ? null : role.getId(),
                role == null ? null : role.getName());
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }

    public UserInfo me(String username) {
        return userInfo(accessService.requireAccount(username));
    }
}
