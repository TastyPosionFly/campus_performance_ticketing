package org.example.campus_performance_ticketing.logic;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.dao.PerformanceSessionRepository;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TicketExpirationTask {
    private final TicketService ticketService;
    private final PerformanceSessionRepository sessionRepository;
    private final PerformanceRepository performanceRepository;

    private static final Logger logger = Logger.getLogger(TicketExpirationTask.class.getName());

    /**
     * 定时任务：每半时处理一次已结束场次的未核销票据
     * 将其状态更新为“已失效”
     */
    @Scheduled(fixedRate = 1800000)
    @Transactional // 建议加事务，保证状态更新一致性
    public void processExpiredTickets() {
        LocalDateTime now = LocalDateTime.now();
        // 只查未结算的场次 (假设 status=2 是已结算)
        List<PerformanceSession> endedSessions = sessionRepository.findByEndTimeBeforeAndStatusNot(now, 2);

        // 记录本轮涉及到的演出ID，稍后统一判断是否需要把演出置为“已结束”
        Set<Long> touchedPerformanceIds = endedSessions.stream()
                .map(s -> s.getPerformance() == null ? null : s.getPerformance().getId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        for (PerformanceSession session : endedSessions) {
            try {
                // 1. 失效票据
                ticketService.expireUnusedTickets(session.getId());

                // 2. 标记场次为已结算 (防止下次重复查)
                session.setStatus(2);
                sessionRepository.save(session);

            } catch (Exception e) {
                logger.warning("失败处理场次 ID " + session.getId() + " 的过期票据: " + e.getMessage());
            }
        }

        // 3. 如果某演出的所有场次都已经超过结束时间，则把演出置为“已结束”(publishStatus=3)
        for (Long performanceId : touchedPerformanceIds) {
            try {
                boolean hasNotEndedSessions =
                        sessionRepository.existsByPerformanceIdAndEndTimeGreaterThanEqual(performanceId, now);

                if (!hasNotEndedSessions) {
                    Performance p = performanceRepository.findById(performanceId).orElse(null);
                    if (p == null) continue;

                    // 只把“已发布(1)”的演出自动变更为“已结束(3)”
                    if (Integer.valueOf(1).equals(p.getPublishStatus())) {
                        p.setPublishStatus(3);
                        performanceRepository.save(p);
                    }
                }
            } catch (Exception e) {
                logger.warning("更新演出为已结束失败 performanceId=" + performanceId + ": " + e.getMessage());
            }
        }
    }
}