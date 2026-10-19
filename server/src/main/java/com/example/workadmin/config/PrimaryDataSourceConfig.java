package com.example.workadmin.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

/**
 * 主数据源仍然是 spring.datasource 指向的 H2 文件库。
 *
 * <p>因为项目额外注册了远程 MySQL DataSource / TransactionManager，Spring Boot 的
 * DataSource/JPA 自动配置会退让；所以这里显式声明 @Primary 的 H2 数据源和 JPA 事务管理器，
 * 保证本地业务继续使用 H2，而远程聊天使用 remoteChatTransactionManager。</p>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataSourceProperties.class)
public class PrimaryDataSourceConfig {

    @Primary
    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Primary
    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
