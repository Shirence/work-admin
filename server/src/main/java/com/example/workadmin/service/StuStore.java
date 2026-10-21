package com.example.workadmin.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * stu 项目真实数据表的访问层。
 * <p>
 * 这些表（user / sys_role / sys_user_role / sys_menu / sys_role_menu / chat_*）属于另一个项目，
 * 结构和数据都不归中台所有，所以这里**只用显式 SQL**读写，不挂 JPA 实体，
 * 避免 Hibernate 的 ddl-auto=update 去改动对方的表结构。
 */
@Repository
public class StuStore {

    private final JdbcTemplate jdbc;

    public StuStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------- 行结构 ----------

    public record StuUser(long id, String name, String account, String passwd,
                          Long roleId, String roleName, String roleCode, Boolean roleEnabled) {
    }

    public record StuRole(long id, String name, String code, String description,
                          boolean enabled, boolean builtin, long userCount) {
    }

    public record StuMenu(long id, Long parentId, String name, String type, String path,
                          String permission, int sortOrder, boolean enabled, boolean builtin) {
    }

    // ---------- 用户 ----------

    private static final String USER_SELECT = """
            select u.id, u.name, u.account, u.passwd,
                   r.id as role_id, r.name as role_name, r.code as role_code, r.enabled as role_enabled
            from user u
            left join sys_user_role sur on sur.user_id = u.id
            left join sys_role r on r.id = sur.role_id
            """;

    private final RowMapper<StuUser> userMapper = (rs, i) -> new StuUser(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("account"),
            rs.getString("passwd"),
            (Long) rs.getObject("role_id"),
            rs.getString("role_name"),
            rs.getString("role_code"),
            rs.getObject("role_enabled") == null ? null : rs.getBoolean("role_enabled"));

    public List<StuUser> listUsers(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        if (kw.isEmpty()) {
            return dedupeById(jdbc.query(USER_SELECT + " order by u.id asc, r.id asc", userMapper));
        }
        String like = "%" + kw + "%";
        return dedupeById(jdbc.query(USER_SELECT + " where u.account like ? or u.name like ? order by u.id asc, r.id asc",
                userMapper, like, like));
    }

    public Optional<StuUser> findUserByAccount(String account) {
        if (account == null || account.isBlank()) return Optional.empty();
        List<StuUser> rows = jdbc.query(USER_SELECT + " where u.account = ? order by r.id asc", userMapper, account.trim());
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<StuUser> findUserById(long id) {
        List<StuUser> rows = jdbc.query(USER_SELECT + " where u.id = ? order by r.id asc", userMapper, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public boolean accountExists(String account) {
        Integer c = jdbc.queryForObject("select count(*) from user where account = ?", Integer.class, account);
        return c != null && c > 0;
    }

    public long insertUser(String name, String account, String passwdHash, long roleId) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into user(name, account, passwd) values (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, account);
            ps.setString(3, passwdHash);
            return ps;
        }, keys);
        long id = keys.getKey() == null ? -1 : keys.getKey().longValue();
        if (id <= 0) {
            throw new IllegalStateException("新增用户失败：未取得自增 id");
        }
        bindRole(id, roleId);
        return id;
    }

    public void updateUser(long id, String name, String account, String passwdHashOrNull) {
        if (passwdHashOrNull == null) {
            jdbc.update("update user set name = ?, account = ? where id = ?", name, account, id);
        } else {
            jdbc.update("update user set name = ?, account = ?, passwd = ? where id = ?",
                    name, account, passwdHashOrNull, id);
        }
    }

    public void bindRole(long userId, long roleId) {
        jdbc.update("delete from sys_user_role where user_id = ?", userId);
        jdbc.update("insert into sys_user_role(user_id, role_id) values (?, ?)", userId, roleId);
    }

    /** 该用户是否还挂在群主或消息记录上，用于删除前的安全检查。 */
    public String userDataBlocker(long userId) {
        Integer groups = jdbc.queryForObject("select count(*) from chat_group where owner_id = ?", Integer.class, userId);
        if (groups != null && groups > 0) return "该用户是 " + groups + " 个群聊的群主";
        Integer messages = jdbc.queryForObject("select count(*) from chat_message where sender_id = ?", Integer.class, userId);
        if (messages != null && messages > 0) return "该用户有 " + messages + " 条历史消息";
        return null;
    }

    public void deleteUser(long userId) {
        jdbc.update("delete from sys_user_role where user_id = ?", userId);
        jdbc.update("delete from user where id = ?", userId);
    }

    // ---------- 角色 ----------

    private static final String ROLE_SELECT = """
            select r.id, r.name, r.code, r.description, r.enabled, r.builtin,
                   (select count(*) from sys_user_role sur where sur.role_id = r.id) as user_count
            from sys_role r
            """;

    private final RowMapper<StuRole> roleMapper = (rs, i) -> new StuRole(
            rs.getLong("id"), rs.getString("name"), rs.getString("code"), rs.getString("description"),
            rs.getBoolean("enabled"), rs.getBoolean("builtin"), rs.getLong("user_count"));

    public List<StuRole> listRoles() {
        return jdbc.query(ROLE_SELECT + " order by r.id asc", roleMapper);
    }

    public Optional<StuRole> findRoleById(long id) {
        List<StuRole> rows = jdbc.query(ROLE_SELECT + " where r.id = ?", roleMapper, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public Optional<StuRole> findRoleByCode(String code) {
        List<StuRole> rows = jdbc.query(ROLE_SELECT + " where r.code = ?", roleMapper, code);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long insertRole(String name, String code, String description, boolean enabled) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into sys_role(name, code, description, enabled, builtin) values (?, ?, ?, ?, 0)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, code);
            ps.setString(3, description);
            ps.setBoolean(4, enabled);
            return ps;
        }, keys);
        return keys.getKey() == null ? -1 : keys.getKey().longValue();
    }

    public void updateRole(long id, String name, String code, String description, boolean enabled) {
        jdbc.update("update sys_role set name = ?, code = ?, description = ?, enabled = ? where id = ?",
                name, code, description, enabled, id);
    }

    public void deleteRole(long id) {
        jdbc.update("delete from sys_role_menu where role_id = ?", id);
        jdbc.update("delete from sys_role where id = ?", id);
    }

    // ---------- 菜单与角色权限 ----------

    private final RowMapper<StuMenu> menuMapper = (rs, i) -> new StuMenu(
            rs.getLong("id"),
            (Long) rs.getObject("parent_id"),
            rs.getString("name"),
            rs.getString("type"),
            rs.getString("path"),
            rs.getString("permission"),
            rs.getInt("sort_order"),
            rs.getBoolean("enabled"),
            rs.getBoolean("builtin"));

    public List<StuMenu> listMenus() {
        return jdbc.query("select * from sys_menu order by sort_order asc, id asc", menuMapper);
    }

    public Optional<StuMenu> findMenuById(long id) {
        List<StuMenu> rows = jdbc.query("select * from sys_menu where id = ?", menuMapper, id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public long insertMenu(Long parentId, String name, String type, String path, String permission,
                           int sortOrder, boolean enabled) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into sys_menu(parent_id, name, type, path, permission, sort_order, enabled, builtin) "
                            + "values (?, ?, ?, ?, ?, ?, ?, 0)", Statement.RETURN_GENERATED_KEYS);
            ps.setObject(1, parentId);
            ps.setString(2, name);
            ps.setString(3, type);
            ps.setString(4, path);
            ps.setString(5, permission);
            ps.setInt(6, sortOrder);
            ps.setBoolean(7, enabled);
            return ps;
        }, keys);
        return keys.getKey() == null ? -1 : keys.getKey().longValue();
    }

    public void updateMenu(long id, Long parentId, String name, String type, String path, String permission,
                           int sortOrder, boolean enabled) {
        jdbc.update("update sys_menu set parent_id = ?, name = ?, type = ?, path = ?, permission = ?, "
                        + "sort_order = ?, enabled = ? where id = ?",
                parentId, name, type, path, permission, sortOrder, enabled, id);
    }

    public void deleteMenu(long id) {
        jdbc.update("delete from sys_role_menu where menu_id = ?", id);
        jdbc.update("delete from sys_menu where id = ?", id);
    }

    public List<Long> roleMenuIds(long roleId) {
        return jdbc.queryForList("select menu_id from sys_role_menu where role_id = ?", Long.class, roleId);
    }

    /** 角色实际拥有的权限点（去重、忽略空值）。 */
    public List<String> rolePermissions(long roleId) {
        return jdbc.queryForList("""
                select distinct m.permission
                from sys_role_menu rm join sys_menu m on m.id = rm.menu_id
                where rm.role_id = ? and m.enabled = 1 and m.permission is not null and m.permission <> ''
                """, String.class, roleId);
    }

    public void replaceRoleMenus(long roleId, List<Long> menuIds) {
        jdbc.update("delete from sys_role_menu where role_id = ?", roleId);
        for (Long menuId : menuIds) {
            jdbc.update("insert into sys_role_menu(role_id, menu_id) values (?, ?)", roleId, menuId);
        }
    }

    public List<Long> existingMenuIds(List<Long> ids) {
        if (ids.isEmpty()) return List.of();
        String marks = String.join(",", ids.stream().map(x -> "?").toList());
        return jdbc.queryForList("select id from sys_menu where id in (" + marks + ")", Long.class, ids.toArray());
    }

    // ---------- 统计 ----------

    public long countUsers() {
        Long v = jdbc.queryForObject("select count(*) from user", Long.class);
        return v == null ? 0 : v;
    }

    public long countGroups() {
        Long v = jdbc.queryForObject("select count(*) from chat_group", Long.class);
        return v == null ? 0 : v;
    }

    public long countMessages() {
        Long v = jdbc.queryForObject("select count(*) from chat_message", Long.class);
        return v == null ? 0 : v;
    }

    public long countMembers() {
        Long v = jdbc.queryForObject("select count(*) from chat_member", Long.class);
        return v == null ? 0 : v;
    }

    /** 按天统计消息量，key 为 yyyy-MM-dd。 */
    public Map<LocalDate, Long> messagesPerDay(LocalDateTime since) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        jdbc.query("select date(sent_at) d, count(*) c from chat_message where sent_at >= ? group by date(sent_at)",
                rs -> {
                    result.put(rs.getDate("d").toLocalDate(), rs.getLong("c"));
                }, since);
        return result;
    }

    public List<Long> superAdminUserIds() {
        List<Long> ids = new ArrayList<>();
        jdbc.query("""
                select sur.user_id from sys_user_role sur join sys_role r on r.id = sur.role_id
                where r.code = 'super_admin'
                """, rs -> {
            ids.add(rs.getLong(1));
        });
        return ids;
    }

    // ---------- 群聊（stu 的 chat_group / chat_member / chat_message） ----------

    public record StuGroup(long id, String name, long ownerId, String ownerName,
                           LocalDateTime createdAt, long memberCount,
                           String lastContent, String lastSenderName, LocalDateTime lastAt) {
    }

    public record StuMessage(long id, long groupId, long senderId, String senderName,
                             String content, String clientMessageId, LocalDateTime sentAt) {
    }

    public record StuMember(long userId, String name, String account,
                            LocalDateTime joinedAt, boolean owner) {
    }

    private static final String GROUP_SELECT = """
            select g.id, g.name, g.owner_id, o.name as owner_name, g.created_at,
                   (select count(*) from chat_member m where m.group_id = g.id) as member_count,
                   lm.content as last_content,
                   lm.sender_name as last_sender_name,
                   lm.sent_at as last_at
            from chat_group g
            left join user o on o.id = g.owner_id
            left join chat_message lm
                   on lm.id = (select max(x.id) from chat_message x where x.group_id = g.id)
            """;

    private final RowMapper<StuGroup> groupMapper = (rs, i) -> {
        java.sql.Timestamp lastAt = rs.getTimestamp("last_at");
        java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
        return new StuGroup(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("owner_id"),
                rs.getString("owner_name"),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                rs.getLong("member_count"),
                rs.getString("last_content"),
                rs.getString("last_sender_name"),
                lastAt == null ? null : lastAt.toLocalDateTime());
    };

    private final RowMapper<StuMessage> messageMapper = (rs, i) -> {
        java.sql.Timestamp sentAt = rs.getTimestamp("sent_at");
        return new StuMessage(
                rs.getLong("id"),
                rs.getLong("group_id"),
                rs.getLong("sender_id"),
                rs.getString("sender_name"),
                rs.getString("content"),
                rs.getString("client_message_id"),
                sentAt == null ? null : sentAt.toLocalDateTime());
    };

    /** 群列表：userId 为 null 表示不按成员过滤（管理员视角）。 */
    public List<StuGroup> listGroups(Long userId) {
        if (userId == null) {
            return jdbc.query(GROUP_SELECT + " order by coalesce(lm.sent_at, g.created_at) desc", groupMapper);
        }
        return jdbc.query(GROUP_SELECT + """
                 where exists (select 1 from chat_member mm where mm.group_id = g.id and mm.user_id = ?)
                 order by coalesce(lm.sent_at, g.created_at) desc
                """, groupMapper, userId);
    }

    public Optional<StuGroup> findGroup(long groupId) {
        List<StuGroup> rows = jdbc.query(GROUP_SELECT + " where g.id = ?", groupMapper, groupId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    public List<StuMember> groupMembers(long groupId) {
        return jdbc.query("""
                select m.user_id, u.name, u.account, m.joined_at, g.owner_id
                from chat_member m
                join user u on u.id = m.user_id
                join chat_group g on g.id = m.group_id
                where m.group_id = ?
                order by (m.user_id = g.owner_id) desc, m.joined_at asc, m.user_id asc
                """, (rs, i) -> {
            java.sql.Timestamp joinedAt = rs.getTimestamp("joined_at");
            long userId = rs.getLong("user_id");
            return new StuMember(userId,
                    rs.getString("name"),
                    rs.getString("account"),
                    joinedAt == null ? null : joinedAt.toLocalDateTime(),
                    userId == rs.getLong("owner_id"));
        }, groupId);
    }

    public boolean isMember(long groupId, long userId) {
        Long v = jdbc.queryForObject(
                "select count(*) from chat_member where group_id = ? and user_id = ?", Long.class, groupId, userId);
        return v != null && v > 0;
    }

    /** 按 id 倒序取最近 limit 条，返回时按 id 升序（便于直接渲染）。 */
    public List<StuMessage> listMessages(long groupId, Long beforeId, int limit) {
        List<StuMessage> rows;
        if (beforeId == null) {
            rows = jdbc.query("""
                    select id, group_id, sender_id, sender_name, content, client_message_id, sent_at
                    from chat_message where group_id = ? order by id desc limit ?
                    """, messageMapper, groupId, limit);
        } else {
            rows = jdbc.query("""
                    select id, group_id, sender_id, sender_name, content, client_message_id, sent_at
                    from chat_message where group_id = ? and id < ? order by id desc limit ?
                    """, messageMapper, groupId, beforeId, limit);
        }
        List<StuMessage> asc = new ArrayList<>(rows);
        java.util.Collections.reverse(asc);
        return asc;
    }

    public boolean messageIdExists(long groupId, String clientMessageId) {
        if (clientMessageId == null || clientMessageId.isBlank()) return false;
        Long v = jdbc.queryForObject(
                "select count(*) from chat_message where group_id = ? and client_message_id = ?",
                Long.class, groupId, clientMessageId);
        return v != null && v > 0;
    }

    public StuMessage insertMessage(long groupId, long senderId, String senderName,
                                    String content, String clientMessageId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    insert into chat_message (group_id, sender_id, sender_name, content, client_message_id)
                    values (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, groupId);
            ps.setLong(2, senderId);
            ps.setString(3, senderName);
            ps.setString(4, content);
            ps.setString(5, clientMessageId);
            return ps;
        }, keyHolder);
        Number key = generatedId(keyHolder);
        long id = key == null ? 0 : key.longValue();
        return jdbc.query("""
                select id, group_id, sender_id, sender_name, content, client_message_id, sent_at
                from chat_message where id = ?
                """, messageMapper, id).stream().findFirst()
                .orElse(new StuMessage(id, groupId, senderId, senderName, content, clientMessageId, LocalDateTime.now()));
    }

    public long createGroup(String name, long ownerId, List<Long> memberIds) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "insert into chat_group (name, owner_id) values (?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setLong(2, ownerId);
            return ps;
        }, keyHolder);
        Number key = generatedId(keyHolder);
        long groupId = key == null ? 0 : key.longValue();
        addMember(groupId, ownerId);
        for (Long userId : memberIds) {
            if (userId != null && userId != ownerId) addMember(groupId, userId);
        }
        return groupId;
    }

    public void addMember(long groupId, long userId) {
        if (isMember(groupId, userId)) return;
        // joined_after_id / history_start_id 是 stu 自己的游标，新成员从 0 开始可见全部历史
        jdbc.update("""
                insert into chat_member (group_id, user_id, joined_after_id, history_start_id)
                values (?, ?, 0, 0)
                """, groupId, userId);
    }

    /**
     * 取自增主键。
     * MySQL 只回一个 GENERATED_KEY，H2 会把带默认值的列（如 sent_at）一起回，
     * 这时 {@code getKey()} 会直接抛异常，所以统一按列名找 id。
     */
    private Number generatedId(KeyHolder keyHolder) {
        for (Map<String, Object> row : keyHolder.getKeyList()) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if ("id".equalsIgnoreCase(entry.getKey()) && entry.getValue() instanceof Number n) {
                    return n;
                }
            }
        }
        try {
            return keyHolder.getKey();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private List<StuUser> dedupeById(List<StuUser> rows) {
        Map<Long, StuUser> byId = new LinkedHashMap<>();
        for (StuUser row : rows) {
            byId.putIfAbsent(row.id(), row);
        }
        return new ArrayList<>(byId.values());
    }
}
