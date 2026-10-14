package com.example.workadmin.config;

import com.example.workadmin.entity.User;
import com.example.workadmin.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 初始化两条演示数据，与前端 Mock 保持一致：张三(admin)、李四(user)。
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initUsers(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User("张三", "13800138000", "admin"));
                userRepository.save(new User("李四", "13900139000", "user"));
            }
        };
    }
}
