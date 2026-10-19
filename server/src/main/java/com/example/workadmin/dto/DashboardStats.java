package com.example.workadmin.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 仪表盘真实统计。没有数据时全部返回 0，前端不做假数据兜底。
 */
public record DashboardStats(
        long userCount,
        long accountCount,
        long conversationCount,
        long messageCount,
        long deviceCount,
        long activeDeviceCount,
        List<DailyMessageCount> dailyMessages,
        LocalDateTime fetchedAt
) {

    public record DailyMessageCount(String date, long count) {
    }
}
