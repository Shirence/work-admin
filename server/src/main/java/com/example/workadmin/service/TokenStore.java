package com.example.workadmin.service;

import com.example.workadmin.dto.UserInfo;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 极简 token 存储（内存版）：进程重启即失效。
 */
@Component
public class TokenStore {

    private final Map<String, UserInfo> store = new ConcurrentHashMap<>();

    public String create(UserInfo userInfo) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store.put(token, userInfo);
        return token;
    }

    public UserInfo get(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return store.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            store.remove(token);
        }
    }
}
