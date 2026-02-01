package org.example.campus_performance_ticketing.logic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.*;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceStats;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 演出数据统计与热度计算服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceStatsService {

    private final PerformanceStatsRepository statsRepository;
    private final PerformanceRepository performanceRepository;
    private final PerformanceSessionRepository sessionRepository;
    private final TicketRepository ticketRepository;

    // === 热度权重配置 ===
    private static final double WEIGHT_VIEW = 0.1;      // 浏览量权重 (最低)
    private static final double WEIGHT_SHARE = 2.0;     // 分享权重
    private static final double WEIGHT_COMMENT = 3.0;   // 评论权重
    private static final double WEIGHT_TICKET = 10.0;   // 预约量/售票权重 (高转化)
    private static final double WEIGHT_CHECK_IN = 20.0; // 实际核销/到场权重 (最终质量)

    /**
     * 前端埋点上报：增加浏览量
     * 为了性能，使用原生 SQL 原子更新
     * @param performanceId 演出 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long performanceId) {
        int rows = statsRepository.incrementViewCount(performanceId);
        if (rows == 0) {
            // 如果不存在统计记录，则初始化后再更新
            initStatsRecord(performanceId);
            statsRepository.incrementViewCount(performanceId);
        }
    }

    /**
     * 前端埋点上报：增加分享数
     */
    @Transactional(rollbackFor = Exception.class)
    public void incrementShareCount(Long performanceId) {
        int rows = statsRepository.incrementShareCount(performanceId);
        if (rows == 0) {
            initStatsRecord(performanceId);
            statsRepository.incrementShareCount(performanceId);
        }
    }

    /**
     * 定时任务：每小时更新所有演出的热度值
     * 同时也刷新 售票数 和 核销数
     * Cron 表达式: 0 0 * * * ? (每小时的第0分0秒执行)
     */
    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void updateAllPerformanceHotScore() {
        log.info(">>> 开始执行演出热度统计定时任务...");
        long start = System.currentTimeMillis();

        // 1. 获取所有状态正常的演出 (1-已发布, 3-已结束)
        // 排除草稿、下架、被拒绝的，避免浪费计算资源
        List<Performance> performances = performanceRepository.findByPublishStatusIn(Arrays.asList(1, 3));

        for (Performance p : performances) {
            try {
                updateSinglePerformanceStats(p);
            } catch (Exception e) {
                log.error("统计演出热度失败: id={}, title={}", p.getId(), p.getTitle(), e);
            }
        }

        long end = System.currentTimeMillis();
        log.info("<<< 热度统计任务结束，耗时 {} ms，处理数量: {}", (end - start), performances.size());
    }

    /**
     * 内部核心方法：计算单场演出的各项数据并更新热度
     */
    private void updateSinglePerformanceStats(Performance p) {
        // 获取或初始化统计对象
        PerformanceStats stats = getOrInitStats(p.getId());

        // 1. 统计预约量/售票数
        // 逻辑：调用 SessionRepository 聚合查询 Sum(ticketTotal - ticketSurplus)
        Integer totalSold = sessionRepository.countTotalSoldTickets(p.getId());
        stats.setTicketSoldCount(totalSold != null ? totalSold : 0);

        // 2. 统计实际核销/到场人数
        // 逻辑：调用 TicketRepository 统计该演出下状态为“已使用”或“已评价”的票据
        int checkInCount = ticketRepository.countActualCheckInByPerformanceId(p.getId());
        stats.setTicketCheckInCount(checkInCount);

        // 3. 获取其他互动数据 (View/Share/Like/Comment 通常是累加的，直接取当前值)
        long viewCount = stats.getViewCount() == null ? 0 : stats.getViewCount();
        long shareCount = stats.getShareCount() == null ? 0 : stats.getShareCount();
        long commentCount = stats.getCommentCount() == null ? 0 : stats.getCommentCount();

        // 4. 计算综合热度分
        double score = (viewCount * WEIGHT_VIEW) +
                (shareCount * WEIGHT_SHARE) +
                (commentCount * WEIGHT_COMMENT) +
                (stats.getTicketSoldCount() * WEIGHT_TICKET) +
                (checkInCount * WEIGHT_CHECK_IN);

        // 5. (可选) 时间衰减机制
        // 如果演出已结束超过7天，热度减半，避免老旧内容一直占据榜单
        if (Integer.valueOf(3).equals(p.getPublishStatus())) { // 3-已结束
            if (p.getUpdatedAt() != null && p.getUpdatedAt().isBefore(LocalDateTime.now().minusDays(7))) {
                score = score * 0.5;
            }
        }

        stats.setHotScore(score);
        stats.setUpdateTime(LocalDateTime.now());

        statsRepository.save(stats);
    }

    /**
     * 辅助方法：获取统计对象，没有则初始化
     */
    private PerformanceStats getOrInitStats(Long performanceId) {
        return statsRepository.findByPerformanceId(performanceId)
                .orElseGet(() -> initStatsRecord(performanceId));
    }

    /**
     * 辅助方法：初始化一条全0的记录
     */
    private PerformanceStats initStatsRecord(Long performanceId) {
        // 双重检查，防止并发创建重复记录
        Optional<PerformanceStats> exist = statsRepository.findByPerformanceId(performanceId);
        if (exist.isPresent()) return exist.get();

        Performance p = performanceRepository.findById(performanceId).orElse(null);
        if (p == null) {
            return new PerformanceStats(); // 理论上不会发生
        }

        PerformanceStats stats = new PerformanceStats();
        stats.setPerformance(p);
        stats.setViewCount(0L);
        stats.setShareCount(0L);
        stats.setCommentCount(0L);
        stats.setTicketSoldCount(0);
        stats.setTicketCheckInCount(0);
        stats.setHotScore(0.0);
        return statsRepository.save(stats);
    }
}