package com.example.workadmin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 远程 MySQL 只读观察台配置。
 *
 * <p>凭据放在 server/application-local.yml（已 gitignore）或环境变量中，
 * 不要提交到仓库。远程库只用于查看，主业务仍然写 H2。</p>
 */
@ConfigurationProperties(prefix = "workadmin.remote-mysql")
public class RemoteMysqlProperties {

    /** 默认关闭，避免未配置时启动失败。 */
    private boolean enabled = false;

    private String url = "";

    private String username = "";

    private String password = "";

    private int queryTimeoutSeconds = 10;

    private int maxRows = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }
}
