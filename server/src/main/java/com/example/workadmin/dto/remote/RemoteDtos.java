package com.example.workadmin.dto.remote;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 远程 stu 群聊库的只读视图 DTO。
 * 不包含用户密码字段。
 */
public final class RemoteDtos {

    private RemoteDtos() {
    }

    public record Overview(
            String database,
            String version,
            String hostname,
            long groupCount,
            long memberCount,
            long messageCount,
            long userCount,
            long latencyMs,
            LocalDateTime fetchedAt
    ) {
    }

    public record GroupRow(
            long id,
            String name,
            Integer ownerId,
            String ownerAccount,
            String ownerName,
            LocalDateTime createdAt,
            long memberCount,
            long messageCount,
            LocalDateTime lastMessageAt
    ) {
    }

    public record MemberRow(
            long groupId,
            Integer userId,
            String account,
            String name,
            LocalDateTime joinedAt,
            long joinedAfterId,
            long historyStartId
    ) {
    }

    public record MessageRow(
            long id,
            long groupId,
            String groupName,
            Integer senderId,
            String senderName,
            String senderAccount,
            String content,
            String clientMessageId,
            LocalDateTime sentAt
    ) {
    }

    public record UserRow(
            Integer id,
            String name,
            String account,
            Long roleId,
            String roleName,
            String roleCode
    ) {
    }

    public record GroupDetail(
            GroupRow group,
            List<MemberRow> members
    ) {
    }

    public record MessagePage(
            List<MessageRow> items,
            long nextAfterId,
            boolean hasMore
    ) {
    }
}
