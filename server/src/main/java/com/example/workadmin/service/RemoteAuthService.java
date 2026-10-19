package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.LoginResponse;
import com.example.workadmin.dto.UserInfo;
import com.example.workadmin.dto.remote.RemoteChatDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
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
import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.function.Supplier;

/**
 * 远程 stu 账号登录 / 注册。
 *
 * <p>登录校验远程 user.passwd（三次 MD5，兼容旧单次 MD5）。
 * 注册写入远程 user 表，并绑定 sys_role.code=member 的默认角色，
 * 与 stu 项目的注册行为一致。</p>
 *
 * <p>登录签发本地 TokenStore 中 authSource=REMOTE 的 token；
 * 该 token 只能访问 /api/remote-auth/** 和 /api/remote-chat/**。</p>
 */
@Service
public class RemoteAuthService {

    private static final Logger log = LoggerFactory.getLogger(RemoteAuthService.class);

    private final ObjectProvider<JdbcTemplate> remoteReadJdbcProvider;
    private final ObjectProvider<JdbcTemplate> remoteChatJdbcProvider;
    private final ObjectProvider<PlatformTransactionManager> remoteChatTxProvider;
    private final TokenStore tokenStore;

    public RemoteAuthService(
            @Qualifier("remoteMysqlJdbcTemplate") ObjectProvider<JdbcTemplate> remoteReadJdbcProvider,
            @Qualifier("remoteChatJdbcTemplate") ObjectProvider<JdbcTemplate> remoteChatJdbcProvider,
            @Qualifier("remoteChatTransactionManager") ObjectProvider<PlatformTransactionManager> remoteChatTxProvider,
            TokenStore tokenStore) {
        this.remoteReadJdbcProvider = remoteReadJdbcProvider;
        this.remoteChatJdbcProvider = remoteChatJdbcProvider;
        this.remoteChatTxProvider = remoteChatTxProvider;
        this.tokenStore = tokenStore;
    }

    public LoginResponse login(RemoteChatDtos.LoginRequest request) {
        String account = request.account() == null ? "" : request.account().trim();
        String password = request.password() == null ? "" : request.password();
        if (account.isEmpty() || password.isEmpty()) {
            throw new BizException(400, "请输入远程账号和密码");
        }

        JdbcTemplate jdbc = readJdbc();
        List<RemoteAccountRow> rows;
        try {
            rows = jdbc.query(
                    "SELECT id, name, account, passwd FROM user WHERE account = ? LIMIT 2",
                    (rs, rowNum) -> new RemoteAccountRow(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("account"),
                            rs.getString("passwd")
                    ),
                    account
            );
        } catch (DataAccessException e) {
            throw remoteReadError(e);
        }

        // 远程账号必须唯一；历史重复账号直接拒绝，避免登录到不确定的身份。
        if (rows.size() != 1) {
            throw new BizException(401, "远程账号或密码错误");
        }
        RemoteAccountRow row = rows.get(0);
        if (!matches(password, row.passwordHash())) {
            throw new BizException(401, "远程账号或密码错误");
        }
        return signedIn(row.id(), row.name(), row.account());
    }

    /**
     * 注册远程 stu 账号。
     * 只允许默认 member 角色，不能通过接口指定角色。
     */
    public LoginResponse register(RemoteChatDtos.RegisterRequest request) {
        String account = request.account() == null ? "" : request.account().trim();
        String password = request.password() == null ? "" : request.password();
        String confirmPassword = request.confirmPassword() == null ? "" : request.confirmPassword();
        if (account.isEmpty() || account.length() < 2 || account.length() > 100) {
            throw new BizException(400, "远程账号长度必须为 2-100 个字符");
        }
        if (account.matches(".*\\s.*")) {
            throw new BizException(400, "远程账号不能包含空白字符");
        }
        if (password.length() < 6 || password.length() > 32) {
            throw new BizException(400, "密码长度必须为 6-32 个字符");
        }
        if (!password.equals(confirmPassword)) {
            throw new BizException(400, "两次输入的密码不一致");
        }
        String displayName = request.name() == null ? "" : request.name().trim();
        if (displayName.isEmpty()) {
            displayName = account;
        }
        if (displayName.length() > 100) {
            throw new BizException(400, "昵称不能超过 100 个字符");
        }
        String finalDisplayName = displayName;

        return inTransaction(() -> {
            JdbcTemplate jdbc = chatJdbc();
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM user WHERE account = ?", Integer.class, account);
            if (exists != null && exists > 0) {
                throw new BizException(409, "远程账号已存在");
            }

            List<Long> roleIds = jdbc.queryForList(
                    "SELECT id FROM sys_role WHERE code = 'member' AND enabled = 1 LIMIT 1", Long.class);
            if (roleIds.isEmpty()) {
                throw new BizException(500, "远程默认角色 member 不存在，无法注册");
            }
            long roleId = roleIds.get(0);

            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO user(name, account, passwd) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, finalDisplayName);
                ps.setString(2, account);
                ps.setString(3, hashPassword(password));
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new BizException(500, "远程账号注册失败：未返回用户 ID");
            }
            long userId = key.longValue();
            jdbc.update("INSERT INTO sys_user_role(user_id, role_id) VALUES (?, ?)", userId, roleId);
            return signedIn(userId, finalDisplayName, account);
        });
    }

    public void logout(String token) {
        tokenStore.remove(token);
    }

    private LoginResponse signedIn(long userId, String name, String account) {
        String displayName = name == null || name.isBlank() ? account : name;
        UserInfo userInfo = new UserInfo(
                account,
                "remote",
                List.of(),
                userId,
                displayName,
                "REMOTE"
        );
        String token = tokenStore.create(userInfo);
        return new LoginResponse(token, userInfo);
    }

    private JdbcTemplate readJdbc() {
        JdbcTemplate template = remoteReadJdbcProvider.getIfAvailable();
        if (template == null) {
            throw new BizException(503, "远程 MySQL 未启用，请配置 server/application-local.yml 或 REMOTE_MYSQL_* 环境变量");
        }
        return template;
    }

    private JdbcTemplate chatJdbc() {
        JdbcTemplate template = remoteChatJdbcProvider.getIfAvailable();
        if (template == null) {
            throw new BizException(503, "远程 MySQL 写连接未启用");
        }
        return template;
    }

    private <T> T inTransaction(Supplier<T> action) {
        PlatformTransactionManager manager = remoteChatTxProvider.getIfAvailable();
        if (manager == null) {
            throw new BizException(503, "远程 MySQL 事务管理器未启用");
        }
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try {
            return template.execute(status -> action.get());
        } catch (BizException e) {
            throw e;
        } catch (DuplicateKeyException e) {
            throw new BizException(409, "远程账号已存在");
        } catch (DataAccessException e) {
            throw remoteWriteError(e);
        } catch (TransactionException e) {
            log.warn("Remote register transaction failed: {}", e.getMessage());
            throw new BizException(503, "远程 MySQL 事务失败：" + e.getMessage());
        }
    }

    private BizException remoteReadError(DataAccessException e) {
        String message = e.getMostSpecificCause() == null ? e.getMessage() : e.getMostSpecificCause().getMessage();
        log.warn("Remote login query failed: {}", message);
        return new BizException(503, "远程 MySQL 查询失败：" + message);
    }

    private BizException remoteWriteError(DataAccessException e) {
        String message = e.getMostSpecificCause() == null ? e.getMessage() : e.getMostSpecificCause().getMessage();
        log.warn("Remote register write failed: {}", message);
        return new BizException(503, "远程 MySQL 注册失败：" + message);
    }

    /**
     * 兼容 stu 的两种密码格式：三次 MD5（当前）和单次 MD5（旧数据）。
     * 登录只校验，不改写远程密码。
     */
    private boolean matches(String candidate, String stored) {
        if (stored == null || stored.isBlank()) {
            return false;
        }
        return constantTimeEquals(md5(md5(md5(candidate))), stored)
                || constantTimeEquals(md5(candidate), stored);
    }

    private String hashPassword(String raw) {
        return md5(md5(md5(raw)));
    }

    private String md5(String value) {
        return DigestUtils.md5DigestAsHex(value.getBytes(StandardCharsets.UTF_8));
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.US_ASCII),
                b.getBytes(StandardCharsets.US_ASCII)
        );
    }

    private record RemoteAccountRow(int id, String name, String account, String passwordHash) {
    }
}
