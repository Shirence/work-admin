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
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public RoleService(RoleRepository roleRepository,
                       RoleMenuRepository roleMenuRepository,
                       MenuRepository menuRepository,
                       AccountRepository accountRepository,
                       PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.roleMenuRepository = roleMenuRepository;
        this.menuRepository = menuRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
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

    @Transactional
    public AccountDtos.AccountRow createAccount(AccountDtos.AccountRequest request) {
        String username = accountUsername(request.username());
        String email = accountEmail(request.email());
        String password = request.password() == null ? "" : request.password();
        if (password.length() < 6 || password.length() > 32) {
            throw new BizException(400, "密码长度必须为 6-32 个字符");
        }
        if (accountRepository.findByUsername(username).isPresent()) {
            throw new BizException(409, "账号已存在");
        }
        if (accountRepository.findByEmail(email).isPresent()) {
            throw new BizException(409, "邮箱已存在");
        }
        Role role = request.roleId() == null ? defaultViewerRole() : requireEnabledRole(request.roleId());
        Account account = new Account();
        account.setUsername(username);
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(password));
        account.setRole(role.getCode());
        account.setRoleId(role.getId());
        account.setEnabled(request.enabled() == null || request.enabled());
        account.setCreatedAt(LocalDateTime.now());
        return toAccountRow(accountRepository.save(account));
    }

    @Transactional
    public AccountDtos.AccountRow updateAccount(Long id, AccountDtos.AccountRequest request, String currentUsername) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "账号不存在"));
        Account current = accountRepository.findByUsername(currentUsername).orElse(null);
        Role currentRole = current == null ? null : roleFor(current);
        Role targetRole = roleFor(account);
        boolean currentAdmin = currentRole != null && "admin".equals(currentRole.getCode());
        if (targetRole != null && "admin".equals(targetRole.getCode()) && !currentAdmin) {
            throw new BizException(403, "只有管理员可以修改管理员账号");
        }
        String username = accountUsername(request.username());
        String email = accountEmail(request.email());
        accountRepository.findByUsername(username).ifPresent(other -> {
            if (!other.getId().equals(id)) throw new BizException(409, "账号已存在");
        });
        accountRepository.findByEmail(email).ifPresent(other -> {
            if (!other.getId().equals(id)) throw new BizException(409, "邮箱已存在");
        });
        boolean enabled = request.enabled() == null || request.enabled();
        if (account.getUsername().equals(currentUsername) && !enabled) {
            throw new BizException(409, "不能停用当前登录账号");
        }
        account.setUsername(username);
        account.setEmail(email);
        account.setEnabled(enabled);
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 6 || request.password().length() > 32) {
                throw new BizException(400, "密码长度必须为 6-32 个字符");
            }
            account.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.roleId() != null) {
            Role role = requireEnabledRole(request.roleId());
            account.setRole(role.getCode());
            account.setRoleId(role.getId());
        }
        return toAccountRow(accountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(Long id, String currentUsername) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "账号不存在"));
        if (account.getUsername().equals(currentUsername)) {
            throw new BizException(409, "不能删除当前登录账号");
        }
        accountRepository.delete(account);
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

    public boolean isSelf(Long accountId, String username) {
        return accountRepository.findById(accountId)
                .map(account -> account.getUsername().equals(username))
                .orElse(false);
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

    private String accountUsername(String raw) {
        String username = raw == null ? "" : raw.trim();
        if (username.length() < 2 || username.length() > 50) {
            throw new BizException(400, "账号长度必须为 2-50 个字符");
        }
        if (username.matches(".*\\s.*")) {
            throw new BizException(400, "账号不能包含空白字符");
        }
        return username;
    }

    private String accountEmail(String raw) {
        String email = raw == null ? "" : raw.trim();
        if (email.isEmpty() || email.length() > 100 || !email.contains("@")) {
            throw new BizException(400, "请填写正确的邮箱");
        }
        return email;
    }

    private Role defaultViewerRole() {
        return roleRepository.findByCode("viewer")
                .orElseThrow(() -> new BizException(500, "默认角色 viewer 不存在"));
    }

    private Role requireEnabledRole(Long roleId) {
        Role role = requireRole(roleId);
        if (Boolean.FALSE.equals(role.getEnabled())) {
            throw new BizException(400, "角色已停用，不能绑定");
        }
        return role;
    }

    private AccountDtos.AccountRow toAccountRow(Account account) {
        Role role = roleFor(account);
        return new AccountDtos.AccountRow(account.getId(), account.getUsername(), account.getEmail(),
                role == null ? account.getRoleId() : role.getId(),
                role == null ? null : role.getName(),
                role == null ? account.getRole() : role.getCode(),
                account.getEnabled());
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
