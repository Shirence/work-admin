package com.example.workadmin.service;

import com.example.workadmin.common.ChatConstants;
import com.example.workadmin.dto.DashboardStats;
import com.example.workadmin.repository.AccountRepository;
import com.example.workadmin.repository.ChatConversationRepository;
import com.example.workadmin.repository.ChatDeviceRepository;
import com.example.workadmin.repository.ChatMessageRepository;
import com.example.workadmin.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 仪表盘只读取真实的 H2 数据；没有记录时返回 0，不使用前端假数据。
 */
@Service
public class DashboardService {

    private static final int TREND_DAYS = 6;
    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("MM-dd");

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatDeviceRepository deviceRepository;

    public DashboardService(UserRepository userRepository,
                            AccountRepository accountRepository,
                            ChatConversationRepository conversationRepository,
                            ChatMessageRepository messageRepository,
                            ChatDeviceRepository deviceRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats stats() {
        LocalDate today = LocalDate.now();
        LocalDate startDay = today.minusDays(TREND_DAYS - 1L);
        List<LocalDateTime> messageTimes = messageRepository.findCreatedAtAfter(startDay.atStartOfDay());
        Map<LocalDate, Long> byDay = messageTimes.stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate, Collectors.counting()));

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
                userRepository.count(),
                accountRepository.count(),
                conversationRepository.count(),
                messageRepository.count(),
                deviceRepository.count(),
                deviceRepository.countByStatus(ChatConstants.DEVICE_ACTIVE),
                daily,
                LocalDateTime.now()
        );
    }
}
