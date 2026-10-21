package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.common.StuPassword;
import com.example.workadmin.dto.AccountDtos;
import com.example.workadmin.dto.RoleDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 用户与角色管理。
 * <p>
 * 数据全部落在 stu 项目的真实表上：
 * 用户 = `user` + `sys_user_role`，角色 = `sys_role`，授权 = `sys_role_menu`。
 * 中台只是这套数据的一个管理界面，不再维护自己的一套账号。
 */
@Service
public class RoleService {

    private static final Pattern CODE = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_:-]{0,63}$");
    private static final String SUPER_ADMIN = "super_admin";
    private static final String DEFAULT_ROLE = "member";

    private final StuStore store;

    public RoleService(StuStore store) {
        this.store = store;
    }

    // ---------- 角色 ----------

    public List<RoleDtos.RoleRow> list() {
        return store.listRoles().stream()
                .map(r -> new RoleDtos.RoleRow(r.id(), r.name(), r.code(), r.description(),
                        r.enabled(), r.builtin(), r.userCount()))
                .toList();
    }

    public List<RoleDtos.RoleOption> options() {
        return store.listRoles().stream()
                .filter(StuStore.StuRole::enabled)
                .map(r -> new RoleDtos.RoleOption(r.id(), r.name(), r.code()))
                .toList();
    }

    @Transactional
    public RoleDtos.RoleRow create(RoleDtos.RoleRequest request) {
        String name = requireName(request.name());
        String code = requireCode(request.code());
        String description = requireDescription(request.description());
        if (store.findRoleByCode(code).isPresent()) {
            throw new BizException(409, "角色编码已存在");
        }
        boolean enabled = request.enabled() == null || request.enabled();
        long id = store.insertRole(name, code, description, enabled);
        return rowById(id);
    }

    @Transactional
    public RoleDtos.RoleRow update(Long id, RoleDtos.RoleRequest request) {
        StuStore.StuRole role = store.findRoleById(id)
                .orElseThrow(() -> new BizException(404, "角色不存在"));
        String name = requireName(request.name());
        String description = requireDescription(request.description());
        String code = request.code() == null ? role.code() : request.code().trim();
        boolean enabled = request.enabled() == null ? role.enabled() : request.enabled();
        if (role.builtin()) {
            if (!role.code().equals(code)) throw new BizException(409, "内置角色编码不能修改");
            if (enabled != role.enabled()) throw new BizException(409, "内置角色不能停用");
        } else {
            code = requireCode(code);
            if (!code.equals(role.code()) && store.findRoleByCode(code).isPresent()) {
                throw new BizException(409, "角色编码已存在");
            }
        }
        store.updateRole(id, name, code, description, enabled);
        return rowById(id);
    }

    @Transactional
    public void delete(Long id) {
        StuStore.StuRole role = store.findRoleById(id)
                .orElseThrow(() -> new BizException(404, "角色不存在"));
        if (role.builtin()) {
            throw new BizException(409, "内置角色不能删除");
        }
        if (role.userCount() > 0) {
            throw new BizException(409, "仍有 " + role.userCount() + " 个用户绑定该角色，不能删除");
        }
        store.deleteRole(id);
    }

    public List<Long> permissionIds(Long roleId) {
        requireRole(roleId);
        return store.roleMenuIds(roleId);
    }

    @Transactional
    public void savePermissions(Long roleId, List<Long> menuIds) {
        StuStore.StuRole role = requireRole(roleId);
        if (SUPER_ADMIN.equals(role.code())) {
            throw new BizException(409, "super_admin 默认拥有全部权限，无需逐条授权");
        }
        Set<Long> ids = new LinkedHashSet<>();
        if (menuIds != null) {
            List<Long> wanted = menuIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
            List<Long> existed = store.existingMenuIds(wanted);
            if (existed.size() != wanted.size()) {
                throw new BizException(400, "存在无效的菜单节点");
            }
            List<StuStore.StuMenu> all = store.listMenus();
            for (Long menuId : wanted) {
                ids.add(menuId);
                // 自动补齐祖先，避免只授权按钮却没有页面入口
                Long parentId = all.stream().filter(m -> m.id() == menuId).findFirst()
                        .map(StuStore.StuMenu::parentId).orElse(null);
                int guard = 0;
                while (parentId != null && guard++ < 100 && ids.add(parentId)) {
                    final Long current = parentId;
                    parentId = all.stream().filter(m -> m.id() == current).findFirst()
                            .map(StuStore.StuMenu::parentId).orElse(null);
                }
            }
        }
        store.replaceRoleMenus(roleId, new ArrayList<>(ids));
    }

    // ---------- 用户 ----------

    public List<AccountDtos.AccountRow> accounts() {
        return accounts(null);
    }

    public List<AccountDtos.AccountRow> accounts(String keyword) {
        return store.listUsers(keyword).stream().map(RoleService::toRow).toList();
    }

    @Transactional
    public AccountDtos.AccountRow createAccount(AccountDtos.AccountRequest request) {
        String account = requireAccount(request.account());
        String name = (request.name() == null || request.name().isBlank())
                ? account : requireName(request.name());
        String password = request.password() == null ? "" : request.password();
        requirePassword(password);
        if (store.accountExists(account)) {
            throw new BizException(409, "账号已存在");
        }
        StuStore.StuRole role = request.roleId() == null
                ? store.findRoleByCode(DEFAULT_ROLE).orElseThrow(() -> new BizException(500, "默认角色 member 不存在"))
                : requireEnabledRole(request.roleId());
        long id = store.insertUser(name, account, StuPassword.hash(password), role.id());
        return store.findUserById(id).map(RoleService::toRow)
                .orElseThrow(() -> new BizException(500, "新增用户后读取失败"));
    }

    @Transactional
    public AccountDtos.AccountRow updateAccount(Long id, AccountDtos.AccountRequest request, String currentUsername) {
        StuStore.StuUser user = store.findUserById(id)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        boolean targetSuperAdmin = SUPER_ADMIN.equals(user.roleCode());
        boolean currentSuperAdmin = isSuperAdmin(currentUsername);
        if (targetSuperAdmin && !currentSuperAdmin) {
            throw new BizException(403, "只有 super_admin 可以修改 super_admin 的资料");
        }
        String account = request.account() == null || request.account().isBlank()
                ? user.account() : requireAccount(request.account());
        String name = request.name() == null || request.name().isBlank()
                ? (user.name() == null ? account : user.name()) : requireName(request.name());
        if (!account.equals(user.account()) && store.accountExists(account)) {
            throw new BizException(409, "账号已存在");
        }
        String passwdHash = null;
        if (request.password() != null && !request.password().isBlank()) {
            requirePassword(request.password());
            passwdHash = StuPassword.hash(request.password());
        }
        store.updateUser(id, name, account, passwdHash);
        if (request.roleId() != null) {
            StuStore.StuRole role = requireEnabledRole(request.roleId());
            if (SUPER_ADMIN.equals(role.code()) && !currentSuperAdmin) {
                throw new BizException(403, "只有 super_admin 可以授予 super_admin 角色");
            }
            store.bindRole(id, role.id());
        }
        return store.findUserById(id).map(RoleService::toRow)
                .orElseThrow(() -> new BizException(500, "更新用户后读取失败"));
    }

    @Transactional
    public void deleteAccount(Long id, String currentUsername) {
        StuStore.StuUser user = store.findUserById(id)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        if (user.account().equals(currentUsername)) {
            throw new BizException(409, "不能删除当前登录账号");
        }
        if (SUPER_ADMIN.equals(user.roleCode()) && !isSuperAdmin(currentUsername)) {
            throw new BizException(403, "只有 super_admin 可以删除 super_admin 账号");
        }
        if (SUPER_ADMIN.equals(user.roleCode()) && store.superAdminUserIds().size() <= 1) {
            throw new BizException(409, "至少要保留一个 super_admin 账号");
        }
        String blocker = store.userDataBlocker(id);
        if (blocker != null) {
            throw new BizException(409, "该用户还挂着群聊数据（" + blocker + "），先清理再删除");
        }
        store.deleteUser(id);
    }

    public boolean isSelf(Long accountId, String username) {
        return store.findUserById(accountId)
                .map(user -> user.account().equals(username))
                .orElse(false);
    }

    @Transactional
    public AccountDtos.AccountRow bindRole(Long accountId, Long roleId) {
        StuStore.StuUser user = store.findUserById(accountId)
                .orElseThrow(() -> new BizException(404, "用户不存在"));
        StuStore.StuRole role = requireEnabledRole(roleId);
        if (SUPER_ADMIN.equals(user.roleCode()) && !SUPER_ADMIN.equals(role.code())
                && !isSuperAdmin(user.account())) {
            throw new BizException(403, "不能降级 super_admin 账号");
        }
        store.bindRole(accountId, role.id());
        return store.findUserById(accountId).map(RoleService::toRow)
                .orElseThrow(() -> new BizException(500, "绑定角色后读取失败"));
    }

    // ---------- 校验 ----------

    private StuStore.StuRole requireRole(Long roleId) {
        if (roleId == null) throw new BizException(400, "缺少角色 id");
        return store.findRoleById(roleId)
                .orElseThrow(() -> new BizException(404, "角色不存在"));
    }

    private StuStore.StuRole requireEnabledRole(Long roleId) {
        StuStore.StuRole role = requireRole(roleId);
        if (!role.enabled()) {
            throw new BizException(400, "角色已停用，不能绑定");
        }
        return role;
    }

    private boolean isSuperAdmin(String username) {
        return store.findUserByAccount(username)
                .map(user -> SUPER_ADMIN.equals(user.roleCode()))
                .orElse(false);
    }

    private RoleDtos.RoleRow rowById(long id) {
        return store.findRoleById(id)
                .map(r -> new RoleDtos.RoleRow(r.id(), r.name(), r.code(), r.description(),
                        r.enabled(), r.builtin(), r.userCount()))
                .orElseThrow(() -> new BizException(500, "读取角色失败"));
    }

    private String requireName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) throw new BizException(400, "名称不能为空");
        if (name.length() > 100) throw new BizException(400, "名称不能超过 100 字");
        return name;
    }

    private String requireCode(String raw) {
        String code = raw == null ? "" : raw.trim();
        if (!CODE.matcher(code).matches()) {
            throw new BizException(400, "编码需以英文字母开头，最长 64 位");
        }
        return code;
    }

    private String requireDescription(String raw) {
        String description = raw == null ? null : raw.trim();
        if (description != null && description.length() > 255) {
            throw new BizException(400, "说明不能超过 255 字");
        }
        return description;
    }

    private String requireAccount(String raw) {
        String account = raw == null ? "" : raw.trim();
        if (account.length() < 2 || account.length() > 50) {
            throw new BizException(400, "账号长度必须为 2-50 个字符");
        }
        if (account.matches(".*\\s.*")) {
            throw new BizException(400, "账号不能包含空白字符");
        }
        return account;
    }

    private void requirePassword(String password) {
        if (password.length() < 6 || password.length() > 32) {
            throw new BizException(400, "密码长度必须为 6-32 个字符");
        }
    }

    private static AccountDtos.AccountRow toRow(StuStore.StuUser user) {
        return new AccountDtos.AccountRow(user.id(), user.name(), user.account(),
                user.roleId(), user.roleName(), user.roleCode());
    }
}
