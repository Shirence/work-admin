package com.example.workadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * work-admin 工作管理中台 后端入口。
 * 启动后默认监听 http://localhost:8081
 */
@SpringBootApplication
public class WorkAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkAdminApplication.class, args);
    }
}
