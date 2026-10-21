package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.AccessDtos;
import com.example.workadmin.dto.MenuDtos;
import com.example.workadmin.dto.RoleDtos;
import com.example.workadmin.entity.Account;
import com.example.workadmin.entity.Menu;
import com.example.workadmin.entity.Role;
import com.example.workadmin.entity.RoleMenu;
import com.example.workadmin.repository.AccountRepository;
import com.example.workadmin.repository.MenuRepository;
import com.example.workadmin.repository.RoleMenuRepository;
import com.example.workadmin.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 读取登录账号的角色、权限和菜单。
 * 权限不写死在代码里，全部来自 sys_role_menu / sys_menu。
 */
@Service
public class AccessService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;

    public AccessService(AccountRepository accountRepository,
                         RoleRepository roleRepository,
                         RoleMenuRepository roleMenuRepository,
                         MenuRepository menuRepository) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
    }

    public Account requireAccount(String username) {
        return accountRepository.findByUsername(username)
                .orElseThrow(() -> new BizException(401, "账号不存在或已失效"));
    }

    public Role roleFor(Account account) {
        if (account == null) return null;
        if (account.getRoleId() != null) {
            Role role = roleRepository.findById(account.getRoleId()).orElse(null);
            if (role != null) return role;
        }
        if (account.getRole() == null || account.getRole().isBlank()) return null;
        return roleRepository.findByCode(account.getRole()).orElse(null);
    }

    public List<Menu> enabledMenus(Role role) {
        if (role == null || Boolean.FALSE.equals(role.getEnabled())) return List.of();
        List<Long> ids = roleMenuRepository.findByRoleId(role.getId()).stream()
                .map(RoleMenu::getMenuId).toList();
        if (ids.isEmpty()) return List.of();
        return menuRepository.findAllById(ids).stream()
                .filter(m -> Boolean.TRUE.equals(m.getEnabled()))
                .sorted(Comparator.comparing(Menu::getSortOrder).thenComparing(Menu::getId))
                .toList();
    }

    public List<String> permissionsFor(String username) {
        Account account = accountRepository.findByUsername(username).orElse(null);
        Role role = roleFor(account);
        if (role == null || Boolean.FALSE.equals(role.getEnabled())) return List.of();
        Set<String> permissions = new LinkedHashSet<>();
        if ("admin".equals(role.getCode())) {
            permissions.add("*");
        }
        for (Menu menu : enabledMenus(role)) {
            if (menu.getPermission() != null && !menu.getPermission().isBlank()) {
                permissions.add(menu.getPermission());
            }
        }
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
}
