package org.example.campus_performance_ticketing.logic;

import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.PerformanceSessionRepository;
import org.example.campus_performance_ticketing.model.PerformanceSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Component
@RequiredArgsConstructor
public class TicketExpirationTask {
    private final TicketService ticketService;
    private final PerformanceSessionRepository sessionRepository;

    private static final Logger log = Logger.getLogger(TicketExpirationTask.class.getName());

    /**
     * 定时任务：每小时处理一次已结束场次的未核销票据
     * 将其状态更新为“已失效”
     */
    @Scheduled(fixedRate = 3600000)
    public void processExpiredTickets() {
        LocalDateTime now = LocalDateTime.now();
        // 只查未结算的场次 (假设 status=2 是已结算)
        List<PerformanceSession> endedSessions = sessionRepository.findByEndTimeBeforeAndStatusNot(now, 2);

        for (PerformanceSession session : endedSessions) {
            try {
                // 1. 失效票据
                ticketService.expireUnusedTickets(session.getId());

                // 2. 标记场次为已结算 (防止下次重复查)
                session.setStatus(2);
                sessionRepository.save(session);

            } catch (Exception e) {
                log.warning("失败处理场次 ID " + session.getId() + " 的过期票据: " + e.getMessage());
            }
        }
    }
}