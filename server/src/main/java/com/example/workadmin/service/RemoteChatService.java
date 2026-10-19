package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.remote.RemoteChatDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 远程 stu 群聊读写。
 *
 * <p>远程账号登录后，可以查看公开群、加入群聊、读取入群历史边界内的消息、发送消息。
 * 数据直接写入远程 MySQL 的 chat_group/chat_member/chat_message，和 stu 项目共用同一套数据。</p>
 *
 * <p>并发控制：加入和发送前都会对 chat_group 行加 FOR UPDATE 锁，和 stu 项目保持一致；
 * 消息幂等键为 (group_id, sender_id, client_message_id)。</p>
 */
@Service
public class RemoteChatService {

    private static final Logger log = LoggerFactory.getLogger(RemoteChatService.class);

    private static final int MAX_GROUP_ROWS = 200;
    private static final int MAX_MESSAGE_ROWS = 100;

    private static final String GROUP_SQL = """
            SELECT g.id, g.name, g.owner_id,
                   u.name AS owner_name, u.account AS owner_account,
                   g.created_at,
                   (SELECT COUNT(*) FROM chat_member m WHERE m.group_id = g.id) AS member_count,
                   EXISTS(SELECT 1 FROM chat_member m WHERE m.group_id = g.id AND m.user_id = ?) AS joined
            FROM chat_group g
            LEFT JOIN user u ON u.id = g.owner_id
            """;

    private static final String MESSAGE_SQL = """
            SELECT m.id, m.group_id, m.sender_id, m.sender_name,
                   m.content, m.client_message_id, m.sent_at
            FROM chat_message m
            """;

    private static final RowMapper<RemoteChatDtos.GroupRow> GROUP_MAPPER = (rs, rowNum) -> new RemoteChatDtos.GroupRow(
            rs.getLong("id"),
            rs.getString("name"),
            nullableInt(rs, "owner_id"),
            rs.getString("owner_name"),
            rs.getString("owner_account"),
            time(rs, "created_at"),
            rs.getLong("member_count"),
            rs.getBoolean("joined")
    );

    private static final RowMapper<RemoteChatDtos.MemberRow> MEMBER_MAPPER = (rs, rowNum) -> new RemoteChatDtos.MemberRow(
            rs.getLong("group_id"),
            nullableInt(rs, "user_id"),
            time(rs, "joined_at"),
            rs.getLong("joined_after_id"),
            rs.getLong("history_start_id")
    );

    private static final RowMapper<RemoteChatDtos.MessageRow> MESSAGE_MAPPER = (rs, rowNum) -> new RemoteChatDtos.MessageRow(
            rs.getLong("id"),
            rs.getLong("group_id"),
            nullableInt(rs, "sender_id"),
            rs.getString("sender_name"),
            rs.getString("content"),
            rs.getString("client_message_id"),
            time(rs, "sent_at")
    );

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final ObjectProvider<PlatformTransactionManager> transactionManagerProvider;

    public RemoteChatService(
            @Qualifier("remoteChatJdbcTemplate") ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            @Qualifier("remoteChatTransactionManager") ObjectProvider<PlatformTransactionManager> transactionManagerProvider) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.transactionManagerProvider = transactionManagerProvider;
    }

    public List<RemoteChatDtos.GroupRow> groups(long userId, long afterId, Integer limit) {
        return query(() -> jdbc().query(
                GROUP_SQL + " WHERE g.id > ? ORDER BY g.id ASC LIMIT ?",
                GROUP_MAPPER,
                userId,
                Math.max(0L, afterId),
                clamp(limit, MAX_GROUP_ROWS, MAX_GROUP_ROWS)
        ));
    }

    public RemoteChatDtos.GroupRow group(long userId, long groupId) {
        return query(() -> requireGroup(userId, groupId));
    }

    public RemoteChatDtos.GroupRow createGroup(long userId, String name) {
        String groupName = name == null ? "" : name.trim();
        if (groupName.isEmpty() || groupName.length() > 100) {
            throw new BizException(400, "群名称长度必须为 1-100 个字符");
        }
        return inTransaction(() -> {
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc().update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO chat_group(name, owner_id) VALUES (?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setString(1, groupName);
                ps.setInt(2, (int) userId);
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new BizException(500, "创建远程群聊失败：未返回群 ID");
            }
            long groupId = key.longValue();
            insertMember(groupId, userId, 0L, 0L);
            return requireGroup(userId, groupId);
        });
    }

    /**
     * 幂等加入群聊，并记录 stu 项目的入群历史边界：
     * 入群前最近 30 条消息可读，更早的不可读；入群后的消息全部可读。
     */
    public RemoteChatDtos.MemberRow joinGroup(long userId, long groupId) {
        return inTransaction(() -> {
            lockGroup(groupId);
            RemoteChatDtos.MemberRow existing = findMember(userId, groupId);
            if (existing != null) {
                return existing;
            }
            List<Long> recent = jdbc().queryForList(
                    "SELECT id FROM chat_message WHERE group_id = ? ORDER BY id DESC LIMIT 30",
                    Long.class,
                    groupId
            );
            long joinedAfterId = recent.isEmpty() ? 0L : recent.get(0);
            long historyStartId = recent.isEmpty() ? 0L : recent.get(recent.size() - 1);
            insertMember(groupId, userId, joinedAfterId, historyStartId);
            return findMember(userId, groupId);
        });
    }

    public RemoteChatDtos.MessagePage history(long userId, long groupId, long afterId, Integer limit) {
        RemoteChatDtos.MemberRow member = findMember(userId, groupId);
        if (member == null) {
            throw new BizException(403, "请先加入群聊");
        }
        long after = Math.max(0L, afterId);
        int size = clamp(limit, MAX_MESSAGE_ROWS, MAX_MESSAGE_ROWS);
        List<RemoteChatDtos.MessageRow> rows = query(() -> jdbc().query(
                MESSAGE_SQL + " WHERE m.group_id = ? AND m.id >= ? AND m.id > ? ORDER BY m.id ASC LIMIT ?",
                MESSAGE_MAPPER,
                groupId,
                member.historyStartId(),
                after,
                size + 1
        ));
        boolean hasMore = rows.size() > size;
        List<RemoteChatDtos.MessageRow> items = hasMore
                ? new ArrayList<>(rows.subList(0, size))
                : rows;
        long nextAfterId = items.isEmpty() ? after : items.get(items.size() - 1).id();
        return new RemoteChatDtos.MessagePage(items, nextAfterId, hasMore);
    }

    public RemoteChatDtos.MessageRow send(long userId, long groupId, String content, String clientMessageId) {
        String text = content == null ? "" : content;
        if (text.isBlank() || text.length() > 2000) {
            throw new BizException(400, "消息不能为空且不能超过 2000 字符");
        }
        String messageId = clientMessageId == null ? "" : clientMessageId.trim();
        if (!messageId.matches("[a-zA-Z0-9_-]{1,64}")) {
            throw new BizException(400, "clientMessageId 必须是 1-64 位字母、数字、下划线或短横线");
        }
        return inTransaction(() -> {
            lockGroup(groupId);
            if (findMember(userId, groupId) == null) {
                throw new BizException(403, "请先加入群聊");
            }
            RemoteChatDtos.MessageRow existing = findMessageByClientId(userId, groupId, messageId);
            if (existing != null) {
                if (!existing.content().equals(text)) {
                    throw new BizException(409, "消息编号已被其他内容使用");
                }
                return existing;
            }
            Sender sender = findSender(userId);
            if (sender == null) {
                throw new BizException(401, "远程账号已失效");
            }
            LocalDateTime now = LocalDateTime.now();
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbc().update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO chat_message(group_id, sender_id, sender_name, content, client_message_id, sent_at) " +
                                "VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                ps.setLong(1, groupId);
                ps.setInt(2, (int) userId);
                ps.setString(3, sender.displayName());
                ps.setString(4, text);
                ps.setString(5, messageId);
                ps.setTimestamp(6, Timestamp.valueOf(now));
                return ps;
            }, keyHolder);
            Number key = keyHolder.getKey();
            if (key == null) {
                throw new BizException(500, "发送远程消息失败：未返回消息 ID");
            }
            RemoteChatDtos.MessageRow saved = findMessage(key.longValue());
            if (saved == null) {
                throw new BizException(500, "发送远程消息失败：消息不存在");
            }
            return saved;
        });
    }

    private RemoteChatDtos.GroupRow requireGroup(long userId, long groupId) {
        List<RemoteChatDtos.GroupRow> rows = jdbc().query(
                GROUP_SQL + " WHERE g.id = ?", GROUP_MAPPER, userId, groupId);
        if (rows.isEmpty()) {
            throw new BizException(404, "远程群组不存在：" + groupId);
        }
        return rows.get(0);
    }

    private RemoteChatDtos.MemberRow findMember(long userId, long groupId) {
        List<RemoteChatDtos.MemberRow> rows = jdbc().query(
                "SELECT group_id, user_id, joined_at, joined_after_id, history_start_id " +
                        "FROM chat_member WHERE group_id = ? AND user_id = ?",
                MEMBER_MAPPER, groupId, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void insertMember(long groupId, long userId, long joinedAfterId, long historyStartId) {
        jdbc().update(
                "INSERT INTO chat_member(group_id, user_id, joined_after_id, history_start_id) VALUES (?, ?, ?, ?)",
                groupId, userId, joinedAfterId, historyStartId
        );
    }

    private RemoteChatDtos.MessageRow findMessageByClientId(long userId, long groupId, String clientMessageId) {
        List<RemoteChatDtos.MessageRow> rows = jdbc().query(
                MESSAGE_SQL + " WHERE m.group_id = ? AND m.sender_id = ? AND m.client_message_id = ?",
                MESSAGE_MAPPER, groupId, userId, clientMessageId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private RemoteChatDtos.MessageRow findMessage(long messageId) {
        List<RemoteChatDtos.MessageRow> rows = jdbc().query(
                MESSAGE_SQL + " WHERE m.id = ?", MESSAGE_MAPPER, messageId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private Sender findSender(long userId) {
        List<Sender> rows = jdbc().query(
                "SELECT id, name, account FROM user WHERE id = ?",
                (rs, rowNum) -> {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String account = rs.getString("account");
                    String displayName = name == null || name.isBlank() ? account : name;
                    return new Sender(id, name, account, displayName);
                },
                userId
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    private void lockGroup(long groupId) {
        try {
            jdbc().queryForObject("SELECT id FROM chat_group WHERE id = ? FOR UPDATE", Long.class, groupId);
        } catch (EmptyResultDataAccessException e) {
            throw new BizException(404, "远程群组不存在：" + groupId);
        }
    }

    private JdbcTemplate jdbc() {
        JdbcTemplate template = jdbcTemplateProvider.getIfAvailable();
        if (template == null) {
            throw new BizException(503, "远程 MySQL 未启用，请配置 server/application-local.yml 或 REMOTE_MYSQL_* 环境变量");
        }
        return template;
    }

    private <T> T inTransaction(Supplier<T> action) {
        PlatformTransactionManager manager = transactionManagerProvider.getIfAvailable();
        if (manager == null) {
            throw new BizException(503, "远程 MySQL 事务管理器未启用");
        }
        TransactionTemplate template = new TransactionTemplate(manager);
        template.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        try {
            return template.execute(status -> action.get());
        } catch (BizException e) {
            throw e;
        } catch (DataAccessException e) {
            throw remoteError(e);
        } catch (TransactionException e) {
            log.warn("Remote chat transaction failed: {}", e.getMessage());
            throw new BizException(503, "远程 MySQL 事务失败：" + e.getMessage());
        }
    }

    private <T> T query(Supplier<T> action) {
        try {
            return action.get();
        } catch (BizException e) {
            throw e;
        } catch (DataAccessException e) {
            throw remoteError(e);
        }
    }

    private BizException remoteError(DataAccessException e) {
        String message = e.getMostSpecificCause() == null ? e.getMessage() : e.getMostSpecificCause().getMessage();
        log.warn("Remote chat query failed: {}", message);
        return new BizException(503, "远程 MySQL 操作失败：" + message);
    }

    private int clamp(Integer requested, int defaultValue, int max) {
        int value = requested == null ? defaultValue : requested;
        value = Math.max(1, value);
        return Math.min(max, value);
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private record Sender(int id, String name, String account, String displayName) {
    }
}
