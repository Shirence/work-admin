package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.remote.RemoteAdminDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 本地管理员管理远程 MySQL 的 stu 账号。
 * 远程 user 表没有 enabled 字段，状态按账号是否可登录处理；当前只做账号/角色管理。
 */
@Service
public class RemoteAdminService {

    private static final Logger log = LoggerFactory.getLogger(RemoteAdminService.class);

    private final ObjectProvider<JdbcTemplate> readProvider;
    private final ObjectProvider<JdbcTemplate> writeProvider;
    private final ObjectProvider<PlatformTransactionManager> txProvider;

    public RemoteAdminService(
            @Qualifier("remoteMysqlJdbcTemplate") ObjectProvider<JdbcTemplate> readProvider,
            @Qualifier("remoteChatJdbcTemplate") ObjectProvider<JdbcTemplate> writeProvider,
            @Qualifier("remoteChatTransactionManager") ObjectProvider<PlatformTransactionManager> txProvider) {
        this.readProvider = readProvider;
        this.writeProvider = writeProvider;
        this.txProvider = txProvider;
    }

    public List<RemoteAdminDtos.RemoteUserRow> users(String keyword) {
        String k = keyword == null ? "" : keyword.trim();
        JdbcTemplate jdbc = read();
        try {
            List<Map<String, Object>> users = jdbc.queryForList("""
                    SELECT id, name, account FROM user
                    WHERE (? = '' OR name LIKE ? OR account LIKE ?)
                    ORDER BY id DESC LIMIT 500
                    """, k, "%" + k + "%", "%" + k + "%");
            Map<Integer, List<RemoteAdminDtos.RemoteRoleRow>> roles = new HashMap<>();
            jdbc.query("""
                    SELECT ur.user_id, r.id, r.name, r.code, r.enabled
                    FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id
                    ORDER BY ur.user_id, r.id
                    """, rs -> {
                roles.computeIfAbsent(rs.getInt("user_id"), id -> new ArrayList<>())
                        .add(new RemoteAdminDtos.RemoteRoleRow(
                                rs.getInt("id"), rs.getString("name"), rs.getString("code"), rs.getBoolean("enabled")));
            });
            List<RemoteAdminDtos.RemoteUserRow> rows = new ArrayList<>();
            for (Map<String, Object> user : users) {
                int id = ((Number) user.get("id")).intValue();
                rows.add(new RemoteAdminDtos.RemoteUserRow(
                        id,
                        (String) user.get("name"),
                        (String) user.get("account"),
                        roles.getOrDefault(id, List.of())
                ));
            }
            return rows;
        } catch (DataAccessException e) {
            throw remoteError("查询远程用户失败", e);
        }
    }

    public List<RemoteAdminDtos.RemoteRoleRow> roles() {
        try {
            return read().query("""
                    SELECT id, name, code, enabled FROM sys_role ORDER BY id
                    """, (rs, rowNum) -> new RemoteAdminDtos.RemoteRoleRow(
                    rs.getInt("id"), rs.getString("name"), rs.getString("code"), rs.getBoolean("enabled")));
        } catch (DataAccessException e) {
            throw remoteError("查询远程角色失败", e);
        }
    }

    public RemoteAdminDtos.RemoteUserRow create(RemoteAdminDtos.RemoteUserRequest request) {
        String name = trim(request.name());
        String account = trim(request.account());
        String password = request.password() == null ? "" : request.password();
        validateAccount(account);
        if (password.length() < 6 || password.length() > 32) {
            throw new BizException(400, "远程密码长度必须为 6-32 个字符");
        }
        if (name == null) name = account;
        String finalName = name;
        int roleId = request.roleId() == null ? defaultMemberRole() : request.roleId();
        return inTransaction(() -> {
            JdbcTemplate jdbc = write();
            Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE account = ?", Integer.class, account);
            if (exists != null && exists > 0) throw new BizException(409, "远程账号已存在");
            requireRole(jdbc, roleId);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO user(name, account, passwd) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, finalName);
                ps.setString(2, account);
                ps.setString(3, hash(password));
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) throw new BizException(500, "创建远程用户失败：未返回用户 ID");
            int userId = key.intValue();
            jdbc.update("INSERT INTO sys_user_role(user_id, role_id) VALUES (?, ?)", userId, roleId);
            return row(jdbc, userId, finalName, account);
        });
    }

    public RemoteAdminDtos.RemoteUserRow update(int userId, RemoteAdminDtos.RemoteUserRequest request) {
        String name = trim(request.name());
        String account = trim(request.account());
        String password = request.password() == null ? "" : request.password();
        validateAccount(account);
        if (name == null) name = account;
        if (!password.isEmpty() && (password.length() < 6 || password.length() > 32)) {
            throw new BizException(400, "远程密码长度必须为 6-32 个字符");
        }
        String finalName = name;
        return inTransaction(() -> {
            JdbcTemplate jdbc = write();
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE account = ? AND id <> ?", Integer.class, account, userId);
            if (exists != null && exists > 0) throw new BizException(409, "远程账号已存在");
            int updated = password.isEmpty()
                    ? jdbc.update("UPDATE user SET name = ?, account = ? WHERE id = ?", finalName, account, userId)
                    : jdbc.update("UPDATE user SET name = ?, account = ?, passwd = ? WHERE id = ?",
                    finalName, account, hash(password), userId);
            if (updated == 0) throw new BizException(404, "远程用户不存在");
            if (request.roleId() != null) {
                requireRole(jdbc, request.roleId());
                replaceRole(jdbc, userId, request.roleId());
            }
            return row(jdbc, userId, finalName, account);
        });
    }

    public RemoteAdminDtos.RemoteUserRow bindRole(int userId, int roleId) {
        return inTransaction(() -> {
            JdbcTemplate jdbc = write();
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, name, account FROM user WHERE id = ?", userId);
            if (rows.isEmpty()) throw new BizException(404, "远程用户不存在");
            requireRole(jdbc, roleId);
            replaceRole(jdbc, userId, roleId);
            Map<String, Object> row = rows.get(0);
            return row(jdbc, userId, (String) row.get("name"), (String) row.get("account"));
        });
    }

    public void delete(int userId) {
        inTransaction(() -> {
            JdbcTemplate jdbc = write();
            Integer groups = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM chat_group WHERE owner_id = ?", Integer.class, userId);
            if (groups != null && groups > 0) {
                throw new BizException(409, "该远程用户仍是群主，请先转移或删除其群聊");
            }
            jdbc.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
            int deleted = jdbc.update("DELETE FROM user WHERE id = ?", userId);
            if (deleted == 0) throw new BizException(404, "远程用户不存在");
            return null;
        });
    }

    private RemoteAdminDtos.RemoteUserRow row(JdbcTemplate jdbc, int userId, String name, String account) {
        Map<Integer, List<RemoteAdminDtos.RemoteRoleRow>> roles = new LinkedHashMap<>();
        jdbc.query("""
                SELECT r.id, r.name, r.code, r.enabled
                FROM sys_user_role ur JOIN sys_role r ON r.id = ur.role_id
                WHERE ur.user_id = ? ORDER BY r.id
                """, rs -> {
            roles.computeIfAbsent(userId, id -> new ArrayList<>())
                    .add(new RemoteAdminDtos.RemoteRoleRow(
                            rs.getInt("id"), rs.getString("name"), rs.getString("code"), rs.getBoolean("enabled")));
        }, userId);
        return new RemoteAdminDtos.RemoteUserRow(userId, name, account, roles.getOrDefault(userId, List.of()));
    }

    private int defaultMemberRole() {
        List<Integer> ids = read().queryForList(
                "SELECT id FROM sys_role WHERE code = 'member' AND enabled = 1 LIMIT 1", Integer.class);
        if (ids.isEmpty()) throw new BizException(500, "远程默认角色 member 不存在");
        return ids.get(0);
    }

    private void requireRole(JdbcTemplate jdbc, int roleId) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM sys_role WHERE id = ?", Integer.class, roleId);
        if (count == null || count == 0) throw new BizException(400, "远程角色不存在");
    }

    private void replaceRole(JdbcTemplate jdbc, int userId, int roleId) {
        jdbc.update("DELETE FROM sys_user_role WHERE user_id = ?", userId);
        jdbc.update("INSERT INTO sys_user_role(user_id, role_id) VALUES (?, ?)", userId, roleId);
    }

    private void validateAccount(String account) {
        if (account == null || account.length() < 2 || account.length() > 100) {
            throw new BizException(400, "远程账号长度必须为 2-100 个字符");
        }
        if (account.matches(".*\\s.*")) {
            throw new BizException(400, "远程账号不能包含空白字符");
        }
    }

    private String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String hash(String raw) {
        String once = DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
        String twice = DigestUtils.md5DigestAsHex(once.getBytes(StandardCharsets.UTF_8));
        return DigestUtils.md5DigestAsHex(twice.getBytes(StandardCharsets.UTF_8));
    }

    private JdbcTemplate read() {
        JdbcTemplate jdbc = readProvider.getIfAvailable();
        if (jdbc == null) throw new BizException(503, "远程 MySQL 未启用");
        return jdbc;
    }

    private JdbcTemplate write() {
        JdbcTemplate jdbc = writeProvider.getIfAvailable();
        if (jdbc == null) throw new BizException(503, "远程 MySQL 写连接未启用");
        return jdbc;
    }

    private <T> T inTransaction(Supplier<T> action) {
        PlatformTransactionManager manager = txProvider.getIfAvailable();
        if (manager == null) throw new BizException(503, "远程 MySQL 事务管理器未启用");
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try {
            return template.execute(status -> action.get());
        } catch (BizException e) {
            throw e;
        } catch (TransactionException e) {
            throw remoteError("远程 MySQL 事务失败", e);
        } catch (DataAccessException e) {
            throw remoteError("远程 MySQL 写入失败", e);
        }
    }

    private BizException remoteError(String message, Exception e) {
        String detail = e.getMessage();
        if (e instanceof DataAccessException dae && dae.getMostSpecificCause() != null) {
            detail = dae.getMostSpecificCause().getMessage();
        }
        log.warn("{}: {}", message, detail);
        return new BizException(503, message + "：" + detail);
    }
}
