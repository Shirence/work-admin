package com.example.workadmin.service;

import com.example.workadmin.dto.DashboardStats;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * 仪表盘只读真实数据（stu 的 user / chat_group / chat_message / chat_member）。
 * 没有记录时返回 0，不做假数据兜底。
 */
@Service
public class DashboardService {

    private static final int TREND_DAYS = 6;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final StuStore store;

    public DashboardService(StuStore store) {
        this.store = store;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(TREND_DAYS - 1L);
        Map<LocalDate, Long> byDay = store.messagesPerDay(startDay.atStartOfDay());

        List<DashboardStats.DailyMessageCount> daily = IntStream.range(0, TREND_DAYS)
                .mapToObj(i -> {
                    LocalDate day = startDay.plusDays(i);
                    return new DashboardStats.DailyMessageCount(
                            day.format(DAY_LABEL),
                            byDay.getOrDefault(day, 0L)
                    );
                })
                .toList();

        return new DashboardStats(
                store.countUsers(),
                store.countGroups(),
                store.countMessages(),
                store.countMembers(),
                daily,
                roleDistribution(),
                LocalDateTime.now()
        );
    }

    private List<DashboardStats.RoleCount> roleDistribution() {
        return store.listRoles().stream()
                .map(role -> new DashboardStats.RoleCount(role.name(), role.code(), role.userCount()))
                .toList();
    }
}
