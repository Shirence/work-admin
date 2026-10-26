package com.example.workadmin.service;

import com.example.workadmin.config.WorkAdminProperties;
import com.example.workadmin.dto.UserInfo;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.codec.TypedJsonJacksonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * token 存 Redis：`work-admin:login:token:{token}` → {@link UserInfo} 的 JSON。
 * <p>
 * 因为 token 不在进程内存里，所以服务重启（甚至换个实例部署）后，
 * 只要 Redis 里的 key 还在、没过期，前端拿着旧 token 照样能访问。
 */
@Component
@ConditionalOnProperty(name = "work-admin.auth.token-store", havingValue = "redis", matchIfMissing = true)
public class RedisTokenStore implements TokenStore {

    private static final Logger log = LoggerFactory.getLogger(RedisTokenStore.class);

    /** 统一前缀：方便在 redis-cli 里一眼认出中台的 key，也方便整体清理。 */
    public static final String KEY_PREFIX = "work-admin:login:token:";

    /** 按 UserInfo 类型反序列化，避免读回来变成 LinkedHashMap。 */
    private static final TypedJsonJacksonCodec CODEC = new TypedJsonJacksonCodec(UserInfo.class);

    private final RedissonClient redisson;
    private final Duration ttl;

    public RedisTokenStore(RedissonClient redisson, WorkAdminProperties properties) {
        this.redisson = redisson;
        this.ttl = properties.getAuth().getTokenTtl();
    }

    @Override
    public String create(UserInfo userInfo) {
        String token = UUID.randomUUID().toString().replace("-", "");
        bucket(token).set(userInfo, ttl);
        log.debug("token 写入 Redis：{}{}，有效期 {}", KEY_PREFIX, token, ttl);
        return token;
    }

    @Override
    public UserInfo get(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        RBucket<UserInfo> bucket = bucket(token);
        UserInfo userInfo = bucket.get();
        if (userInfo != null) {
            // 滑动过期：只要用户还在用，就把有效期往后推，避免正干活时被踢下线
            bucket.expire(ttl);
        }
        return userInfo;
    }

    @Override
    public void remove(String token) {
        if (token != null && !token.isBlank()) {
            bucket(token).delete();
        }
    }

    private RBucket<UserInfo> bucket(String token) {
        return redisson.getBucket(KEY_PREFIX + token, CODEC);
    }
}
