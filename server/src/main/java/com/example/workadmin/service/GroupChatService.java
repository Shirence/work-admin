package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.GroupChatDtos;
import com.example.workadmin.entity.Account;
import com.example.workadmin.entity.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 群聊业务。
 * <p>
 * 消息的真相在 stu 的 `chat_group` / `chat_member` / `chat_message` 三张表里，
 * 而且那边是**明文**存储（项目自带 WebSocket 群聊就是这么落库的）。
 * 所以中台这边不再做端到端加密，直接读写同一份数据：
 * 中台发的消息，stu 前端能看到；stu 发的消息，中台也能看到。
 */
@Service
public class GroupChatService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** 一次最多取多少条历史消息。 */
    private static final int MAX_LIMIT = 200;

    private static final int MAX_CONTENT = 2000;

    private final StuStore store;
    private final AccessService accessService;

    public GroupChatService(StuStore store, AccessService accessService) {
        this.store = store;
        this.accessService = accessService;
    }

    /** 当前登录用户在 stu 里的真实用户。 */
    public StuStore.StuUser requireUser(String account) {
        if (account == null || account.isBlank()) throw new BizException(401, "未登录");
        return store.findUserByAccount(account.trim())
                .orElseThrow(() -> new BizException(401, "账号不存在或已失效"));
    }

    /**
     * 群列表。普通用户只看自己加入的群；super_admin 看全站所有群（可观察、可管理）。
     */
    public List<GroupChatDtos.GroupRow> groups(String account) {
        StuStore.StuUser me = requireUser(account);
        boolean admin = isSuperAdmin(me);
        List<StuStore.StuGroup> rows = store.listGroups(admin ? null : me.id());
        // 一次查出「我加入了哪些群」，别在循环里逐行查库
        java.util.Set<Long> joinedIds = store.groupIdsOf(me.id());
        List<GroupChatDtos.GroupRow> result = new ArrayList<>();
        for (StuStore.StuGroup g : rows) {
            boolean joined = admin || joinedIds.contains(g.id());
            result.add(toRow(g, joined));
        }
        return result;
    }

    public GroupChatDtos.GroupDetail detail(String account, long groupId) {
        StuStore.StuUser me = requireUser(account);
        StuStore.StuGroup group = store.findGroup(groupId)
                .orElseThrow(() -> new BizException(404, "群不存在"));
        boolean admin = isSuperAdmin(me);
        boolean joined = store.isMember(groupId, me.id());
        if (!joined && !admin) {
            throw new BizException(403, "你不在这个群里");
        }
        List<GroupChatDtos.MemberRow> members = store.groupMembers(groupId).stream()
                .map(m -> new GroupChatDtos.MemberRow(m.userId(), m.name(), m.account(), m.owner(), time(m.joinedAt())))
                .toList();
        return new GroupChatDtos.GroupDetail(toRow(group, joined || admin), members);
    }

    /**
     * 历史消息，按 id 升序返回。beforeId 用于往上翻页。
     */
    public List<GroupChatDtos.MessageRow> messages(String account, long groupId, Long beforeId, Integer limit) {
        StuStore.StuUser me = requireUser(account);
        requireVisible(me, groupId);
        int size = limit == null || limit <= 0 ? 50 : Math.min(limit, MAX_LIMIT);
        return store.listMessages(groupId, beforeId, size).stream()
                .map(m -> new GroupChatDtos.MessageRow(
                        m.id(), m.groupId(), m.senderId(), m.senderName(),
                        m.content(), m.senderId() == me.id(), time(m.sentAt())))
                .toList();
    }

    @Transactional
    public GroupChatDtos.MessageRow send(String account, long groupId, GroupChatDtos.SendRequest request) {
        StuStore.StuUser me = requireUser(account);
        requireVisible(me, groupId);
        String content = request == null || request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) throw new BizException(400, "消息内容不能为空");
        if (content.length() > MAX_CONTENT) throw new BizException(400, "消息不能超过 2000 字");
        String clientId = request.clientMessageId() == null || request.clientMessageId().isBlank()
                ? null : request.clientMessageId().trim();
        if (clientId != null && store.messageIdExists(groupId, clientId)) {
            // 网络重试导致的重复提交：直接返回已存在的那条，不重复入库
            return store.listMessages(groupId, null, MAX_LIMIT).stream()
                    .filter(m -> clientId.equals(m.clientMessageId()))
                    .findFirst()
                    .map(m -> new GroupChatDtos.MessageRow(m.id(), m.groupId(), m.senderId(), m.senderName(),
                            m.content(), true, time(m.sentAt())))
                    .orElseThrow(() -> new BizException(409, "消息重复"));
        }
        StuStore.StuMessage saved = store.insertMessage(groupId, me.id(), me.name(), content, clientId);
        return new GroupChatDtos.MessageRow(saved.id(), saved.groupId(), saved.senderId(), saved.senderName(),
                saved.content(), true, time(saved.sentAt()));
    }

    @Transactional
    public GroupChatDtos.GroupDetail create(String account, GroupChatDtos.CreateRequest request) {
        StuStore.StuUser me = requireUser(account);
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new BizException(400, "请填写群名称");
        if (name.length() > 100) throw new BizException(400, "群名称不能超过 100 字");
        List<Long> members = request.memberIds() == null ? List.of() : request.memberIds();
        List<Long> valid = new ArrayList<>();
        for (Long id : members) {
            if (id == null || id == me.id()) continue;
            if (store.findUserById(id).isEmpty()) throw new BizException(400, "成员不存在：" + id);
            valid.add(id);
        }
        long groupId = store.createGroup(name, me.id(), valid);
        return detail(account, groupId);
    }

    /** 主动加入公开群（demo 场景：任何登录用户都能加入已存在的群）。 */
    @Transactional
    public GroupChatDtos.GroupDetail join(String account, long groupId) {
        StuStore.StuUser me = requireUser(account);
        store.findGroup(groupId).orElseThrow(() -> new BizException(404, "群不存在"));
        store.addMember(groupId, me.id());
        return detail(account, groupId);
    }

    private void requireVisible(StuStore.StuUser me, long groupId) {
        if (store.findGroup(groupId).isEmpty()) throw new BizException(404, "群不存在");
        if (!store.isMember(groupId, me.id()) && !isSuperAdmin(me)) {
            throw new BizException(403, "你不在这个群里");
        }
    }

    private boolean isSuperAdmin(StuStore.StuUser user) {
        Account account = accessService.requireAccount(user.account());
        Role role = accessService.roleFor(account);
        return role != null && "super_admin".equals(role.getCode());
    }

    private GroupChatDtos.GroupRow toRow(StuStore.StuGroup g, boolean joined) {
        return new GroupChatDtos.GroupRow(g.id(), g.name(), g.ownerId(), g.ownerName(),
                (int) g.memberCount(), g.lastContent(), g.lastSenderName(), time(g.lastAt()), joined);
    }

    private String time(java.time.LocalDateTime value) {
        return value == null ? null : TIME.format(value);
    }
}
