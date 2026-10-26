package com.example.workadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * application.yml 里 {@code work-admin} 段的配置。
 * <p>
 * 只关心两件事：登录 token 存在哪、Redis 怎么连。
 */
@Component
@ConfigurationProperties(prefix = "work-admin")
public class WorkAdminProperties {

    private final Auth auth = new Auth();
    private final Redis redis = new Redis();

    public Auth getAuth() {
        return auth;
    }

    public Redis getRedis() {
        return redis;
    }

    /** 登录相关的配置。 */
    public static class Auth {

        /** token 存放位置：{@code redis}（默认，重启不丢）或 {@code memory}（进程内存，重启即失效）。 */
        private String tokenStore = "redis";

        /** token 有效期；每次带 token 的请求都会续期（滑动过期）。 */
        private Duration tokenTtl = Duration.ofDays(7);

        public String getTokenStore() {
            return tokenStore;
        }

        public void setTokenStore(String tokenStore) {
            this.tokenStore = tokenStore;
        }

        public Duration getTokenTtl() {
            return tokenTtl;
        }

        public void setTokenTtl(Duration tokenTtl) {
            this.tokenTtl = tokenTtl;
        }
    }

    /** Redis 连接配置。 */
    public static class Redis {

        /** 单机地址，格式 {@code redis://host:port}。 */
        private String address = "redis://127.0.0.1:6379";

        /** 没设密码就留空。 */
        private String password = "";

        /** 逻辑库编号，0~15。 */
        private int database = 0;

        /** 建立连接的超时时间，超时直接启动失败，避免卡住整个服务。 */
        private Duration connectTimeout = Duration.ofSeconds(3);

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getDatabase() {
            return database;
        }

        public void setDatabase(int database) {
            this.database = database;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }
    }
}
