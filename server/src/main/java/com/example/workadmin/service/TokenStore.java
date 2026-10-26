package com.example.workadmin.service;

import com.example.workadmin.dto.UserInfo;

/**
 * 登录 token 的存放位置。
 * <p>
 * 两种实现由 {@code work-admin.auth.token-store} 决定：
 * {@link RedisTokenStore}（默认，重启服务后 token 依然有效）、
 * {@link MemoryTokenStore}（进程内存，仅供离线开发 / 测试）。
 */
public interface TokenStore {

    /** 为登录成功的用户签发 token，并保存对应的身份信息。 */
    String create(UserInfo userInfo);

    /** 用 token 换回身份信息；token 不存在或已过期返回 {@code null}。 */
    UserInfo get(String token);

    /** 退出登录：让 token 立即失效。 */
    void remove(String token);
}
