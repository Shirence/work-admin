package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.AccountDtos;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class RoleService {

    private static final Pattern CODE = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_:-]{0,63}$");

    private final RoleRepository roleRepository;
    private final RoleMenuRepository roleMenuRepository;
    private final MenuRepository menuRepository;
    private final AccountRepository accountRepository;

    public RoleService(RoleRepository roleRepository,
                       RoleMenuRepository roleMenuRepository,
                       MenuRepository menuRepository,
                       AccountRepository accountRepository) {
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
        this.accountRepository = accountRepository;
    }

    public List<RoleDtos.RoleRow> list() {
        return roleRepository.findAllByOrderByIdAsc().stream()
                .map(role -> new RoleDtos.RoleRow(role.getId(), role.getName(), role.getCode(),
                        role.getDescription(), role.getEnabled(), role.getBuiltin(),
                        accountRepository.countByRoleId(role.getId())))
                .toList();
    }

    public List<RoleDtos.RoleOption> options() {
        return roleRepository.findByEnabledTrueOrderByIdAsc().stream()
                .map(role -> new RoleDtos.RoleOption(role.getId(), role.getName(), role.getCode()))
                .toList();
    }

    @Transactional
    public RoleDtos.RoleRow create(RoleDtos.RoleRequest request) {
        Role role = new Role();
        apply(role, request, false);
        role.setBuiltin(false);
        role.setCreatedAt(LocalDateTime.now());
        return toRow(roleRepository.save(role));
    }

    @Transactional
    public RoleDtos.RoleRow update(Long id, RoleDtos.RoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "角色不存在"));
        apply(role, request, Boolean.TRUE.equals(role.getBuiltin()));
        return toRow(roleRepository.save(role));
    }

    @Transactional
    public void delete(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "角色不存在"));
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new BizException(409, "内置角色不能删除");
        }
        if (accountRepository.countByRoleId(id) > 0) {
            throw new BizException(409, "仍有账号绑定该角色，不能删除");
        }
        roleMenuRepository.deleteByRoleId(id);
        roleRepository.delete(role);
    }

    public List<Long> permissionIds(Long roleId) {
        requireRole(roleId);
        return roleMenuRepository.findByRoleId(roleId).stream().map(RoleMenu::getMenuId).toList();
    }

    @Transactional
    public void savePermissions(Long roleId, List<Long> menuIds) {
        requireRole(roleId);
        Set<Long> ids = new LinkedHashSet<>();
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                if (menuId == null) continue;
                Menu menu = menuRepository.findById(menuId)
                        .orElseThrow(() -> new BizException(400, "菜单节点不存在：" + menuId));
                ids.add(menu.getId());
                // 自动补齐祖先，避免只授权按钮却没有页面入口。
                Long parentId = menu.getParentId();
                long guard = 0;
                while (parentId != null && guard++ < 100 && ids.add(parentId)) {
                    Menu parent = menuRepository.findById(parentId).orElse(null);
                    parentId = parent == null ? null : parent.getParentId();
                }
            }
        }
        roleMenuRepository.deleteByRoleId(roleId);
        roleMenuRepository.saveAll(ids.stream().map(menuId -> new RoleMenu(roleId, menuId)).toList());
    }

    public List<AccountDtos.AccountRow> accounts() {
        return accountRepository.findAllByOrderByIdAsc().stream().map(account -> {
            Role role = roleFor(account);
            return new AccountDtos.AccountRow(account.getId(), account.getUsername(), account.getEmail(),
                    role == null ? null : role.getId(),
                    role == null ? null : role.getName(),
                    role == null ? account.getRole() : role.getCode(),
                    account.getEnabled());
        }).toList();
    }

    @Transactional
    public AccountDtos.AccountRow bindRole(Long accountId, Long roleId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new BizException(404, "账号不存在"));
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(400, "角色不存在"));
        if (Boolean.FALSE.equals(role.getEnabled())) {
            throw new BizException(400, "角色已停用，不能绑定");
        }
        account.setRoleId(role.getId());
        account.setRole(role.getCode());
        accountRepository.save(account);
        return new AccountDtos.AccountRow(account.getId(), account.getUsername(), account.getEmail(),
                role.getId(), role.getName(), role.getCode(), account.getEnabled());
    }

    private void apply(Role role, RoleDtos.RoleRequest request, boolean builtin) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new BizException(400, "角色名不能为空");
        if (name.length() > 100) throw new BizException(400, "角色名不能超过 100 字");
        String code = request.code() == null ? "" : request.code().trim();
        if (!CODE.matcher(code).matches()) {
            throw new BizException(400, "角色编码需以英文字母开头，最长 64 位");
        }
        String description = request.description() == null ? null : request.description().trim();
        if (description != null && description.length() > 255) {
            throw new BizException(400, "角色说明不能超过 255 字");
        }
        boolean enabled = request.enabled() == null || request.enabled();
        if (builtin) {
            if (!role.getCode().equals(code)) throw new BizException(409, "内置角色编码不能修改");
            if (!Boolean.valueOf(enabled).equals(role.getEnabled())) {
                throw new BizException(409, "内置角色不能停用");
            }
        } else {
            roleRepository.findByCode(code).ifPresent(existing -> {
                if (!existing.getId().equals(role.getId())) {
                    throw new BizException(409, "角色编码已存在");
                }
            });
        }
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        role.setEnabled(enabled);
    }

    private Role requireRole(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new BizException(404, "角色不存在"));
    }

    private Role roleFor(Account account) {
        if (account.getRoleId() != null) {
            Role role = roleRepository.findById(account.getRoleId()).orElse(null);
            if (role != null) return role;
        }
        return account.getRole() == null ? null : roleRepository.findByCode(account.getRole()).orElse(null);
    }

    private RoleDtos.RoleRow toRow(Role role) {
        return new RoleDtos.RoleRow(role.getId(), role.getName(), role.getCode(), role.getDescription(),
                role.getEnabled(), role.getBuiltin(), accountRepository.countByRoleId(role.getId()));
    }
}
