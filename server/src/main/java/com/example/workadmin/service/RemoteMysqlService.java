package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.config.RemoteMysqlProperties;
import com.example.workadmin.dto.remote.RemoteDtos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 远程 stu 群聊库的只读查询。
 *
 * <p>该服务只执行 SELECT，不提供任何写入接口。连接池被标记为 readOnly；
 * 远程库不可用时返回 503，不影响 H2 主业务。</p>
 */
@Service
public class RemoteMysqlService {

    private static final Logger log = LoggerFactory.getLogger(RemoteMysqlService.class);

    private static final String GROUP_SQL = """
            SELECT g.id, g.name, g.owner_id,
                   u.account AS owner_account, u.name AS owner_name,
                   g.created_at,
                   (SELECT COUNT(*) FROM chat_member m WHERE m.group_id = g.id) AS member_count,
                   (SELECT COUNT(*) FROM chat_message msg WHERE msg.group_id = g.id) AS message_count,
                   (SELECT MAX(msg.sent_at) FROM chat_message msg WHERE msg.group_id = g.id) AS last_message_at
            FROM chat_group g
            LEFT JOIN user u ON u.id = g.owner_id
            """;

    private static final String MEMBER_SQL = """
            SELECT m.group_id, m.user_id, u.account, u.name,
                   m.joined_at, m.joined_after_id, m.history_start_id
            FROM chat_member m
            LEFT JOIN user u ON u.id = m.user_id
            WHERE m.group_id = ?
            ORDER BY m.joined_at ASC, m.user_id ASC
            """;

    private static final String MESSAGE_SQL = """
            SELECT m.id, m.group_id, g.name AS group_name,
                   m.sender_id, m.sender_name, u.account AS sender_account,
                   m.content, m.client_message_id, m.sent_at
            FROM chat_message m
            LEFT JOIN chat_group g ON g.id = m.group_id
            LEFT JOIN user u ON u.id = m.sender_id
            """;

    private static final String USER_SQL = """
            SELECT u.id, u.name, u.account,
                   r.id AS role_id, r.name AS role_name, r.code AS role_code
            FROM user u
            LEFT JOIN sys_user_role ur ON ur.user_id = u.id
            LEFT JOIN sys_role r ON r.id = ur.role_id
            """;

    private static final RowMapper<RemoteDtos.GroupRow> GROUP_MAPPER = (rs, rowNum) -> new RemoteDtos.GroupRow(
            rs.getLong("id"),
            rs.getString("name"),
            nullableInt(rs, "owner_id"),
            rs.getString("owner_account"),
            rs.getString("owner_name"),
            time(rs, "created_at"),
            rs.getLong("member_count"),
            rs.getLong("message_count"),
            time(rs, "last_message_at")
    );

    private static final RowMapper<RemoteDtos.MemberRow> MEMBER_MAPPER = (rs, rowNum) -> new RemoteDtos.MemberRow(
            rs.getLong("group_id"),
            nullableInt(rs, "user_id"),
            rs.getString("account"),
            rs.getString("name"),
            time(rs, "joined_at"),
            rs.getLong("joined_after_id"),
            rs.getLong("history_start_id")
    );

    private static final RowMapper<RemoteDtos.MessageRow> MESSAGE_MAPPER = (rs, rowNum) -> new RemoteDtos.MessageRow(
            rs.getLong("id"),
            rs.getLong("group_id"),
            rs.getString("group_name"),
            nullableInt(rs, "sender_id"),
            rs.getString("sender_name"),
            rs.getString("sender_account"),
            rs.getString("content"),
            rs.getString("client_message_id"),
            time(rs, "sent_at")
    );

    private static final RowMapper<RemoteDtos.UserRow> USER_MAPPER = (rs, rowNum) -> new RemoteDtos.UserRow(
            nullableInt(rs, "id"),
            rs.getString("name"),
            rs.getString("account"),
            nullableLong(rs, "role_id"),
            rs.getString("role_name"),
            rs.getString("role_code")
    );

    private final ObjectProvider<JdbcTemplate> jdbcTemplateProvider;
    private final RemoteMysqlProperties properties;

    public RemoteMysqlService(
            @Qualifier("remoteMysqlJdbcTemplate") ObjectProvider<JdbcTemplate> jdbcTemplateProvider,
            RemoteMysqlProperties properties) {
        this.jdbcTemplateProvider = jdbcTemplateProvider;
        this.properties = properties;
    }

    public RemoteDtos.Overview overview() {
        return query(() -> {
            long start = System.currentTimeMillis();
            RemoteDtos.Overview row = jdbc().queryForObject("""
                    SELECT DATABASE() AS db_name, VERSION() AS db_version, @@hostname AS host_name,
                           (SELECT COUNT(*) FROM chat_group) AS group_count,
                           (SELECT COUNT(*) FROM chat_member) AS member_count,
                           (SELECT COUNT(*) FROM chat_message) AS message_count,
                           (SELECT COUNT(*) FROM user) AS user_count
                    """, (rs, rowNum) -> new RemoteDtos.Overview(
                    rs.getString("db_name"),
                    rs.getString("db_version"),
                    rs.getString("host_name"),
                    rs.getLong("group_count"),
                    rs.getLong("member_count"),
                    rs.getLong("message_count"),
                    rs.getLong("user_count"),
                    0L,
                    null
            ));
            if (row == null) {
                throw new BizException(503, "远程 MySQL 未返回概览数据");
            }
            return new RemoteDtos.Overview(
                    row.database(), row.version(), row.hostname(),
                    row.groupCount(), row.memberCount(), row.messageCount(), row.userCount(),
                    System.currentTimeMillis() - start,
                    LocalDateTime.now()
            );
        });
    }

    public List<RemoteDtos.GroupRow> groups(String keyword, Integer limit) {
        return query(() -> {
            StringBuilder sql = new StringBuilder(GROUP_SQL);
            List<Object> args = new ArrayList<>();
            String kw = normalizeKeyword(keyword);
            if (!kw.isEmpty()) {
                sql.append(" WHERE g.name LIKE ? OR u.account LIKE ? OR u.name LIKE ?");
                String like = "%" + kw + "%";
                args.add(like);
                args.add(like);
                args.add(like);
            }
            sql.append(" ORDER BY g.id DESC LIMIT ?");
            args.add(clampLimit(limit, 200));
            return jdbc().query(sql.toString(), GROUP_MAPPER, args.toArray());
        });
    }

    public RemoteDtos.GroupDetail group(long groupId) {
        return query(() -> {
            List<RemoteDtos.GroupRow> groups = jdbc().query(
                    GROUP_SQL + " WHERE g.id = ?", GROUP_MAPPER, groupId);
            if (groups.isEmpty()) {
                throw new BizException(404, "远程群组不存在：" + groupId);
            }
            List<RemoteDtos.MemberRow> members = jdbc().query(MEMBER_SQL, MEMBER_MAPPER, groupId);
            return new RemoteDtos.GroupDetail(groups.get(0), members);
        });
    }

    public RemoteDtos.MessagePage groupMessages(long groupId, Long afterId, Integer limit) {
        return query(() -> {
            long after = afterId == null ? 0L : Math.max(0L, afterId);
            int size = clampLimit(limit, 200);
            List<RemoteDtos.MessageRow> items = jdbc().query(
                    MESSAGE_SQL + " WHERE m.group_id = ? AND m.id > ? ORDER BY m.id ASC LIMIT ?",
                    MESSAGE_MAPPER, groupId, after, size);
            long next = items.isEmpty() ? after : items.get(items.size() - 1).id();
            return new RemoteDtos.MessagePage(items, next, items.size() >= size);
        });
    }

    /**
     * 跨群检索最近消息；groupId 为空表示所有群。
     */
    public List<RemoteDtos.MessageRow> recentMessages(String keyword, Long groupId, Integer limit) {
        return query(() -> {
            StringBuilder sql = new StringBuilder(MESSAGE_SQL).append(" WHERE 1 = 1");
            List<Object> args = new ArrayList<>();
            if (groupId != null) {
                sql.append(" AND m.group_id = ?");
                args.add(groupId);
            }
            String kw = normalizeKeyword(keyword);
            if (!kw.isEmpty()) {
                sql.append(" AND (m.content LIKE ? OR m.sender_name LIKE ? OR u.account LIKE ?)");
                String like = "%" + kw + "%";
                args.add(like);
                args.add(like);
                args.add(like);
            }
            sql.append(" ORDER BY m.id DESC LIMIT ?");
            args.add(clampLimit(limit, 100));
            return jdbc().query(sql.toString(), MESSAGE_MAPPER, args.toArray());
        });
    }

    public List<RemoteDtos.UserRow> users(String keyword, Integer limit) {
        return query(() -> {
            StringBuilder sql = new StringBuilder(USER_SQL);
            List<Object> args = new ArrayList<>();
            String kw = normalizeKeyword(keyword);
            if (!kw.isEmpty()) {
                sql.append(" WHERE u.name LIKE ? OR u.account LIKE ?");
                String like = "%" + kw + "%";
                args.add(like);
                args.add(like);
            }
            sql.append(" ORDER BY u.id ASC LIMIT ?");
            args.add(clampLimit(limit, 200));
            return jdbc().query(sql.toString(), USER_MAPPER, args.toArray());
        });
    }

    private JdbcTemplate jdbc() {
        JdbcTemplate template = jdbcTemplateProvider.getIfAvailable();
        if (template == null) {
            throw new BizException(503, "远程 MySQL 未启用，请配置 server/application-local.yml 或 REMOTE_MYSQL_* 环境变量");
        }
        return template;
    }

    private <T> T query(Supplier<T> action) {
        try {
            return action.get();
        } catch (BizException e) {
            throw e;
        } catch (DataAccessException e) {
            String message = e.getMostSpecificCause() == null
                    ? e.getMessage()
                    : e.getMostSpecificCause().getMessage();
            log.warn("Remote MySQL query failed: {}", message);
            throw new BizException(503, "远程 MySQL 查询失败：" + message);
        }
    }

    private int clampLimit(Integer requested, int defaultValue) {
        int value = requested == null ? defaultValue : requested;
        value = Math.max(1, value);
        return Math.min(Math.max(1, properties.getMaxRows()), value);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return "";
        }
        String value = keyword.trim();
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).intValue();
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }

    private static LocalDateTime time(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
