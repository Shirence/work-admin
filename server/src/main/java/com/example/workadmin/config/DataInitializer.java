package com.example.workadmin.config;

import com.example.workadmin.common.RolePermissions;
import com.example.workadmin.entity.Account;
import com.example.workadmin.entity.ChatGroupPolicy;
import com.example.workadmin.entity.Menu;
import com.example.workadmin.entity.Role;
import com.example.workadmin.entity.RoleMenu;
import com.example.workadmin.entity.User;
import com.example.workadmin.repository.AccountRepository;
import com.example.workadmin.repository.ChatGroupPolicyRepository;
import com.example.workadmin.repository.MenuRepository;
import com.example.workadmin.repository.RoleMenuRepository;
import com.example.workadmin.repository.RoleRepository;
import com.example.workadmin.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
                               RoleRepository roleRepository,
                               MenuRepository menuRepository,
                               RoleMenuRepository roleMenuRepository,
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
            seedAccess(roleRepository, menuRepository, roleMenuRepository);
            ensureRemotePermissions(roleRepository, menuRepository, roleMenuRepository);
            // 旧数据没有 roleId 时按 role 编码回填。
            for (Account account : accountRepository.findAll()) {
                if (account.getRoleId() == null) {
                    roleRepository.findByCode(account.getRole()).ifPresent(role -> {
                        account.setRoleId(role.getId());
                        accountRepository.save(account);
                    });
                }
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

    private void seedAccess(RoleRepository roleRepository,
                            MenuRepository menuRepository,
                            RoleMenuRepository roleMenuRepository) {
        Role admin = roleRepository.findByCode("admin").orElseGet(() -> role(roleRepository, "超级管理员", "admin", "拥有全部权限", true));
        Role manager = roleRepository.findByCode("manager").orElseGet(() -> role(roleRepository, "管理员", "manager", "可管理用户与查看数据", true));
        Role viewer = roleRepository.findByCode("viewer").orElseGet(() -> role(roleRepository, "普通用户", "viewer", "只读访问", true));

        if (menuRepository.count() == 0) {
            Menu dashboard = menu(menuRepository, null, "仪表盘", "MENU", "/dashboard", "dashboard:view", 1, true);
            Menu chat = menu(menuRepository, null, "消息", "MENU", "/chat", "chat:access", 2, true);
            Menu system = menu(menuRepository, null, "系统管理", "DIRECTORY", null, null, 10, true);
            Menu user = menu(menuRepository, system.getId(), "用户管理", "MENU", "/user", "user:read", 11, true);
            Menu roles = menu(menuRepository, system.getId(), "角色管理", "MENU", "/roles", "role:read", 12, true);
            Menu menus = menu(menuRepository, system.getId(), "菜单管理", "MENU", "/menus", "menu:read", 13, true);

            Menu userCreate = menu(menuRepository, user.getId(), "新增用户", "BUTTON", null, "user:create", 1, true);
            Menu userUpdate = menu(menuRepository, user.getId(), "编辑用户", "BUTTON", null, "user:update", 2, true);
            Menu userDelete = menu(menuRepository, user.getId(), "删除用户", "BUTTON", null, "user:delete", 3, true);
            Menu userBind = menu(menuRepository, user.getId(), "绑定角色", "BUTTON", null, "user:assign-role", 4, true);
            Menu roleCreate = menu(menuRepository, roles.getId(), "新增角色", "BUTTON", null, "role:create", 1, true);
            Menu roleUpdate = menu(menuRepository, roles.getId(), "编辑角色", "BUTTON", null, "role:update", 2, true);
            Menu roleDelete = menu(menuRepository, roles.getId(), "删除角色", "BUTTON", null, "role:delete", 3, true);
            Menu roleGrant = menu(menuRepository, roles.getId(), "分配权限", "BUTTON", null, "role:grant", 4, true);
            Menu menuCreate = menu(menuRepository, menus.getId(), "新增节点", "BUTTON", null, "menu:create", 1, true);
            Menu menuUpdate = menu(menuRepository, menus.getId(), "编辑节点", "BUTTON", null, "menu:update", 2, true);
            Menu menuDelete = menu(menuRepository, menus.getId(), "删除节点", "BUTTON", null, "menu:delete", 3, true);

            grant(roleMenuRepository, admin, dashboard, chat, system, user, roles, menus,
                    userCreate, userUpdate, userDelete, userBind,
                    roleCreate, roleUpdate, roleDelete, roleGrant,
                    menuCreate, menuUpdate, menuDelete);
            grant(roleMenuRepository, manager, dashboard, chat, system, user, userUpdate);
            grant(roleMenuRepository, viewer, dashboard, chat);
        }
    }

    private void ensureRemotePermissions(RoleRepository roleRepository,
                                         MenuRepository menuRepository,
                                         RoleMenuRepository roleMenuRepository) {
        Menu userPage = menuRepository.findByPermission("user:read").orElse(null);
        Role admin = roleRepository.findByCode("admin").orElse(null);
        if (userPage == null || admin == null) return;

        String[][] defs = {
                {"远程用户查看", "remote:user:read"},
                {"新增远程用户", "remote:user:create"},
                {"编辑远程用户", "remote:user:update"},
                {"删除远程用户", "remote:user:delete"},
                {"绑定远程角色", "remote:user:assign-role"}
        };
        List<Long> granted = roleMenuRepository.findByRoleId(admin.getId()).stream()
                .map(RoleMenu::getMenuId).toList();
        int sort = 10;
        for (String[] def : defs) {
            int order = sort;
            Menu node = menuRepository.findByPermission(def[1]).orElseGet(() ->
                    menu(menuRepository, userPage.getId(), def[0], "BUTTON", null, def[1], order, true));
            if (!granted.contains(node.getId())) {
                roleMenuRepository.save(new RoleMenu(admin.getId(), node.getId()));
            }
            sort++;
        }
    }

    private Role role(RoleRepository repository, String name, String code, String description, boolean builtin) {
        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        role.setEnabled(true);
        role.setBuiltin(builtin);
        role.setCreatedAt(LocalDateTime.now());
        return repository.save(role);
    }

    private Menu menu(MenuRepository repository, Long parentId, String name, String type,
                      String path, String permission, int sortOrder, boolean builtin) {
        Menu menu = new Menu();
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setType(type);
        menu.setPath(path);
        menu.setPermission(permission);
        menu.setSortOrder(sortOrder);
        menu.setEnabled(true);
        menu.setBuiltin(builtin);
        return repository.save(menu);
    }

    private void grant(RoleMenuRepository repository, Role role, Menu... menus) {
        List<RoleMenu> rows = new ArrayList<>();
        for (Menu menu : menus) {
            rows.add(new RoleMenu(role.getId(), menu.getId()));
        }
        repository.saveAll(rows);
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
