package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.PerformanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PerformanceSessionRepository extends JpaRepository<PerformanceSession, Long> {

    /**
     * 查询某演出的所有场次
     */
    List<PerformanceSession> findByPerformanceId(Long performanceId);

    /**
     * 核心功能：排期冲突检测
     * 逻辑：查询指定场地在 [startTime, endTime] 区间内是否已有其他排期
     * 公式：(ExistingStart < NewEnd) AND (ExistingEnd > NewStart)
     *
     * @param venueId 场地 ID
     * @param startTime 拟定开始时间
     * @param endTime 拟定结束时间
     * @return 冲突的场次列表（如果为空说明无冲突）
     */
    @Query("SELECT s FROM PerformanceSession s " +
            "JOIN s.performance p " + // 关键：关联 Performance 表
            "WHERE s.venue.id = :venueId " +
            "AND s.startTime < :endTime " +
            "AND s.endTime > :startTime " +
            // 关键过滤：只把以下状态视为冲突：
            // 0-待审批, 1-已发布
            // 排除：2-审批拒绝, 3-已取消, 4-已下架(看业务), 5-已结束(但时间重叠通常意味着还没结束), 6-被征用
            "AND p.publishStatus IN (0, 1)")
    List<PerformanceSession> findConflicts(@Param("venueId") Long venueId,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);
}