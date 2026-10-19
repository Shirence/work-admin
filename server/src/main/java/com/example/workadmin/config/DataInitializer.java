package com.example.workadmin.config;

import com.example.workadmin.common.RolePermissions;
import com.example.workadmin.entity.Account;
import com.example.workadmin.entity.ChatGroupPolicy;
import com.example.workadmin.entity.User;
import com.example.workadmin.repository.AccountRepository;
import com.example.workadmin.repository.ChatGroupPolicyRepository;
import com.example.workadmin.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

/**
 * 初始化演示数据：业务用户（张三、李四）+ 登录账号（admin / manager / viewer）。
 * 密码统一 123456，存 BCrypt 哈希。
 * 同时保证聊天全局建群策略存在（默认 AUTO_ALLOW）。
 */
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository,
                               AccountRepository accountRepository,
                               ChatGroupPolicyRepository chatGroupPolicyRepository,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User("张三", "13800138000", "admin"));
                userRepository.save(new User("李四", "13900139000", "user"));
            }
            if (accountRepository.count() == 0) {
                accountRepository.save(account("admin", "admin@work-admin.local", "admin", passwordEncoder));
                accountRepository.save(account("manager", "manager@work-admin.local", "manager", passwordEncoder));
                accountRepository.save(account("viewer", "viewer@work-admin.local", "viewer", passwordEncoder));
            }
            if (chatGroupPolicyRepository.count() == 0) {
                ChatGroupPolicy policy = new ChatGroupPolicy();
                policy.setMode("AUTO_ALLOW");
                policy.setUpdatedBy("system");
                policy.setUpdatedAt(LocalDateTime.now());
                chatGroupPolicyRepository.save(policy);
            }
        };
    }

    private Account account(String username, String email, String role, PasswordEncoder encoder) {
        Account a = new Account();
        a.setUsername(username);
        a.setPassword(encoder.encode("123456"));
        a.setEmail(email);
        a.setRole(role);
        a.setEnabled(true);
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }
}
