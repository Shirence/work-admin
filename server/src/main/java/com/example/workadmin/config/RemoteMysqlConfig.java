package com.example.workadmin.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 远程 MySQL 连接池：
 *
 * <ul>
 *   <li><b>remote-mysql-readonly</b>：观察台使用，readOnly=true，只执行 SELECT；</li>
 *   <li><b>remote-mysql-chat</b>：远程 stu 账号聊天使用，可写 chat_group/chat_member/chat_message；</li>
 *   <li>两个池都设置初始化失败不阻塞启动，远程库不可用时由业务层返回 503。</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RemoteMysqlProperties.class)
public class RemoteMysqlConfig {

    @Bean(name = "remoteMysqlDataSource", destroyMethod = "close")
    @ConditionalOnProperty(prefix = "workadmin.remote-mysql", name = "enabled", havingValue = "true")
    public HikariDataSource remoteMysqlDataSource(RemoteMysqlProperties properties) {
        return buildDataSource(properties, "remote-mysql-readonly", true, 3);
    }

    @Bean(name = "remoteMysqlJdbcTemplate")
    @ConditionalOnProperty(prefix = "workadmin.remote-mysql", name = "enabled", havingValue = "true")
    public JdbcTemplate remoteMysqlJdbcTemplate(
            @Qualifier("remoteMysqlDataSource") DataSource dataSource,
            RemoteMysqlProperties properties) {
        return buildJdbcTemplate(dataSource, properties);
    }

    /**
     * 远程聊天写连接池。远程 stu 账号登录后，加入群聊/发消息使用这个池。
     */
    @Bean(name = "remoteChatDataSource", destroyMethod = "close")
    @ConditionalOnProperty(prefix = "workadmin.remote-mysql", name = "enabled", havingValue = "true")
    public HikariDataSource remoteChatDataSource(RemoteMysqlProperties properties) {
        return buildDataSource(properties, "remote-mysql-chat", false, 5);
    }

    @Bean(name = "remoteChatJdbcTemplate")
    @ConditionalOnProperty(prefix = "workadmin.remote-mysql", name = "enabled", havingValue = "true")
    public JdbcTemplate remoteChatJdbcTemplate(
            @Qualifier("remoteChatDataSource") DataSource dataSource,
            RemoteMysqlProperties properties) {
        return buildJdbcTemplate(dataSource, properties);
    }

    @Bean(name = "remoteChatTransactionManager")
    @ConditionalOnProperty(prefix = "workadmin.remote-mysql", name = "enabled", havingValue = "true")
    public PlatformTransactionManager remoteChatTransactionManager(
            @Qualifier("remoteChatDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    private HikariDataSource buildDataSource(RemoteMysqlProperties properties,
                                             String poolName,
                                             boolean readOnly,
                                             int maxPoolSize) {
        if (properties.getUrl() == null || properties.getUrl().isBlank()) {
            throw new IllegalStateException(
                    "workadmin.remote-mysql.enabled=true 但 url 为空，请配置 server/application-local.yml 或 REMOTE_MYSQL_URL");
        }
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setJdbcUrl(properties.getUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setReadOnly(readOnly);
        config.setAutoCommit(true);
        config.setMaximumPoolSize(Math.max(1, maxPoolSize));
        config.setMinimumIdle(0);
        config.setConnectionTimeout(5_000);
        config.setValidationTimeout(3_000);
        config.setIdleTimeout(60_000);
        config.setMaxLifetime(300_000);
        // 远程库暂时不可用时也允许应用启动，查询时再报 503。
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private JdbcTemplate buildJdbcTemplate(DataSource dataSource, RemoteMysqlProperties properties) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setQueryTimeout(Math.max(1, properties.getQueryTimeoutSeconds()));
        template.setMaxRows(Math.max(1, properties.getMaxRows()));
        template.setFetchSize(200);
        return template;
    }
}
