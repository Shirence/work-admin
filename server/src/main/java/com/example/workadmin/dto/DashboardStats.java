package com.example.workadmin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仪表盘真实统计，全部来自 stu 项目的业务表。
 * 没有数据时返回 0，前端不做假数据兜底。
 */
public record DashboardStats(
        /** `user` 表用户数 */
        long userCount,
        /** `chat_group` 群聊数 */
        long groupCount,
        /** `chat_message` 消息数 */
        long messageCount,
        /** `chat_member` 群成员关系数 */
        long memberCount,
        List<DailyMessageCount> dailyMessages,
        List<RoleCount> roleDistribution,
        LocalDateTime fetchedAt
) {

    public record DailyMessageCount(String date, long count) {
    }

    public record RoleCount(String name, String code, long count) {
    }
}
