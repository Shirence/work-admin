package com.example.workadmin.service;

import com.example.workadmin.dto.UserInfo;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存版 token 存储：进程一重启 token 全失效。
 * <p>
 * 只在 {@code work-admin.auth.token-store=memory} 时启用（离线开发、集成测试）。
 * 正常开发/部署请用默认的 Redis 实现。
 */
@Component
@ConditionalOnProperty(name = "work-admin.auth.token-store", havingValue = "memory")
public class MemoryTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(MemoryTokenStore.class);

    private final Map<String, UserInfo> store = new ConcurrentHashMap<>();

    @PostConstruct
    void warn() {
        log.warn("当前使用内存版 token 存储，服务重启后需要重新登录（如需持久化请设置 work-admin.auth.token-store=redis）");
    }

    @Override
    public String create(UserInfo userInfo) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, userInfo);
        return token;
    }

    @Override
    public UserInfo get(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return store.get(token);
    }

    @Override
    public void remove(String token) {
        if (token != null) {
            store.remove(token);
        }
    }
}
