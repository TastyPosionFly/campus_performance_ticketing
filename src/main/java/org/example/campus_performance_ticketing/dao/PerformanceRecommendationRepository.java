package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.PerformanceRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PerformanceRecommendationRepository extends JpaRepository<PerformanceRecommendation, Long> {


    /**
     * 检查某演出在特定推荐类型下是否已存在
     * 允许同一演出同时存在于 Type=1 和 Type=2，但在同一 Type 下唯一
     */
    boolean existsByPerformanceIdAndType(Long performanceId, Integer type);

    /**
     * 查询当前时刻有效的推荐列表
     * 条件：Type匹配 AND (Start为空或已开始) AND (End为空或未结束) AND (演出已发布)
     * 排序：权重倒序 > ID倒序
     */
    @Query("SELECT r FROM PerformanceRecommendation r " +
            "JOIN FETCH r.performance p " +
            "WHERE r.type = :type " +
            "AND (r.startTime IS NULL OR r.startTime <= :now) " +
            "AND (r.endTime IS NULL OR r.endTime >= :now) " +
            "AND p.publishStatus = 1 " +
            "ORDER BY r.sortOrder DESC, r.id DESC")
    List<PerformanceRecommendation> findActiveRecommendations(@Param("type") Integer type,
                                                              @Param("now") LocalDateTime now);

    /**
     * 简单的查询所有，按创建时间倒序
     */
    List<PerformanceRecommendation> findByTypeOrderByCreateTimeDesc(Integer type);
}