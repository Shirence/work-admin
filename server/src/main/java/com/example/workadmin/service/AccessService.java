package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.AccessDtos;
import com.example.workadmin.dto.MenuDtos;
import com.example.workadmin.entity.Account;
import com.example.workadmin.entity.Menu;
import com.example.workadmin.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 读取登录用户的角色、权限和菜单。
 * <p>
 * 中台不再自带账号体系：身份来自 stu 的 `user` 表，
 * 权限来自 `sys_user_role` → `sys_role_menu` → `sys_menu`。
 * 只有 super_admin 走通配符 `*`（该角色在 sys_role_menu 里没有逐条授权）。
 */
@Service
public class AccessService {

    /** 任何登录用户都有的基础权限，避免连概览页都进不去。 */
    private static final String BASELINE_PERMISSION = "dashboard:view";

    private static final String SUPER_ADMIN = "super_admin";

    private final StuStore store;

    public AccessService(StuStore store) {
        this.store = store;
    }

    public Account requireAccount(String username) {
        StuStore.StuUser user = store.findUserByAccount(username)
                .orElseThrow(() -> new BizException(401, "账号不存在或已失效"));
        return toAccount(user);
    }

    public Account findAccount(String username) {
        return store.findUserByAccount(username).map(this::toAccount).orElse(null);
    }

    public boolean accountExists(String username) {
        return username != null && !username.isBlank() && store.accountExists(username.trim());
    }

    public Role roleFor(Account account) {
        if (account == null || account.getRoleId() == null) return null;
        return store.findRoleById(account.getRoleId()).map(this::toRole).orElse(null);
    }

    public List<Menu> enabledMenus(Role role) {
        if (role == null || Boolean.FALSE.equals(role.getEnabled())) return List.of();
        List<Menu> menus = new ArrayList<>();
        if (SUPER_ADMIN.equals(role.getCode())) {
            for (StuStore.StuMenu m : store.listMenus()) {
                if (m.enabled()) menus.add(toMenu(m));
            }
            return menus;
        }
        List<Long> ids = store.roleMenuIds(role.getId());
        if (ids.isEmpty()) return List.of();
        for (StuStore.StuMenu m : store.listMenus()) {
            if (m.enabled() && ids.contains(m.id())) menus.add(toMenu(m));
        }
        return menus;
    }

    public List<String> permissionsFor(String username) {
        Account account = findAccount(username);
        if (account == null) return List.of();
        Role role = roleFor(account);
        Set<String> permissions = new LinkedHashSet<>();
        if (role == null || Boolean.FALSE.equals(role.getEnabled())) {
            return List.of();
        }
        if (SUPER_ADMIN.equals(role.getCode())) {
            permissions.add("*");
        } else {
            permissions.addAll(store.rolePermissions(role.getId()));
        }
        permissions.add(BASELINE_PERMISSION);
        return new ArrayList<>(permissions);
    }

    public boolean hasPermission(String username, String permission) {
        List<String> permissions = permissionsFor(username);
        return permissions.contains("*") || permissions.contains(permission);
    }

    public void require(String username, String permission) {
        if (!hasPermission(username, permission)) {
            throw new BizException(403, "没有权限执行该操作");
        }
    }

    @Transactional(readOnly = true)
    public AccessDtos.AccessResponse access(String username) {
        Account account = requireAccount(username);
        Role role = roleFor(account);
        List<MenuDtos.MenuRow> menus = enabledMenus(role).stream()
                .map(MenuService::toRow)
                .toList();
        AccessDtos.RoleDto roleDto = role == null ? null
                : new AccessDtos.RoleDto(role.getId(), role.getName(), role.getCode(), role.getEnabled());
        return new AccessDtos.AccessResponse(roleDto, permissionsFor(username), menus);
    }

    private Account toAccount(StuStore.StuUser user) {
        Account account = new Account();
        account.setId(user.id());
        account.setUsername(user.account());
        account.setPassword(user.passwd());
        account.setRole(user.roleCode());
        account.setRoleId(user.roleId());
        account.setEnabled(true);
        return account;
    }

    private Role toRole(StuStore.StuRole role) {
        Role value = new Role();
        value.setId(role.id());
        value.setName(role.name());
        value.setCode(role.code());
        value.setDescription(role.description());
        value.setEnabled(role.enabled());
        value.setBuiltin(role.builtin());
        return value;
    }

    private Menu toMenu(StuStore.StuMenu menu) {
        Menu value = new Menu();
        value.setId(menu.id());
        value.setParentId(menu.parentId());
        value.setName(menu.name());
        value.setType(menu.type());
        value.setPath(menu.path());
        value.setPermission(menu.permission());
        value.setSortOrder(menu.sortOrder());
        value.setEnabled(menu.enabled());
        value.setBuiltin(menu.builtin());
        return value;
    }
}
