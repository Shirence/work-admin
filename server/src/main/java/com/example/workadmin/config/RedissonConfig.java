package com.example.workadmin.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Redisson 客户端（单机模式）。
 * <p>
 * {@code work-admin.auth.token-store=memory} 时整个配置不生效，连都不会去连 Redis，
 * 这样离线开发或单元测试不会因为「本机没起 Redis」而起不来。
 */
@Configuration
@ConditionalOnProperty(name = "work-admin.auth.token-store", havingValue = "redis", matchIfMissing = true)
public class RedissonConfig {

    private static final Logger log = LoggerFactory.getLogger(RedissonConfig.class);

    private final WorkAdminProperties properties;

    public RedissonConfig(WorkAdminProperties properties) {
        this.properties = properties;
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        WorkAdminProperties.Redis redis = properties.getRedis();
        int connectTimeout = (int) redis.getConnectTimeout().toMillis();

        Config config = new Config();
        // 跨版本/跨机器都能读的 JSON 编码，别用默认的二进制编码（换版本就读不出来了）
        config.setCodec(org.redisson.codec.JsonJacksonCodec.INSTANCE);

        SingleServerConfig server = config.useSingleServer()
                .setAddress(redis.getAddress())
                .setDatabase(redis.getDatabase())
                .setConnectTimeout(connectTimeout)
                // 命令超时给连接超时的两倍，避免网络抖动时误判断线
                .setTimeout(connectTimeout * 2)
                .setRetryAttempts(3)
                // 只存 token，用不着默认的 24 条连接；共用服务器的 Redis 别把连接数吃满
                .setConnectionPoolSize(8)
                .setConnectionMinimumIdleSize(2);
        if (StringUtils.hasText(redis.getPassword())) {
            server.setPassword(redis.getPassword());
        }

        RedissonClient client = Redisson.create(config);
        log.info("Redisson 已连接 {}（database={}），登录 token 将保存在 Redis", redis.getAddress(), redis.getDatabase());
        return client;
    }
}
