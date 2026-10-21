package com.example.workadmin.dto;

import java.util.List;

/**
 * 群聊接口的数据结构。
 * 字段命名对齐前端（驼峰），时间统一为 ISO-8601 本地时间字符串，前端直接 new Date() 解析。
 */
public class GroupChatDtos {

    /** 群列表一行。 */
    public record GroupRow(long id,
                           String name,
                           long ownerId,
                           String ownerName,
                           int memberCount,
                           String lastMessage,
                           String lastSender,
                           String lastAt,
                           boolean joined) {
    }

    /** 群成员一行。 */
    public record MemberRow(long id,
                            String name,
                            String account,
                            boolean owner,
                            String joinedAt) {
    }

    /** 消息一行。mine 表示是不是当前登录用户发的。 */
    public record MessageRow(long id,
                             long groupId,
                             long senderId,
                             String senderName,
                             String content,
                             boolean mine,
                             String sentAt) {
    }

    /** 单群详情：群信息 + 成员列表。 */
    public record GroupDetail(GroupRow group, List<MemberRow> members) {
    }

    /** 发送消息请求。clientMessageId 用于前端重试去重。 */
    public record SendRequest(String content, String clientMessageId) {
    }

    /** 建群请求。 */
    public record CreateRequest(String name, List<Long> memberIds) {
    }
}
