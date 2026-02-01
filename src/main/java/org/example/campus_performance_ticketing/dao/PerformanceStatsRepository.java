package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.PerformanceStats;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PerformanceStatsRepository extends JpaRepository<PerformanceStats, Long> {

    Optional<PerformanceStats> findByPerformanceId(Long performanceId);

    // 【新增】批量查询统计数据
    List<PerformanceStats> findByPerformanceIdIn(List<Long> performanceIds);

    /**
     * 查询热门演出统计数据，按热度分降序排序
     * @param pageable
     * @return
     */
    @Query("SELECT s FROM PerformanceStats s " +
            "JOIN s.performance p " +
            "WHERE p.publishStatus IN (1, 3) " +
            "ORDER BY s.hotScore DESC")
    List<PerformanceStats> findTopHot(Pageable pageable);

    /**
     * 浏览量原子更新，返回值改为 int
     * @param performanceId
     * @return
     */
    @Modifying
    @Query("UPDATE PerformanceStats s SET s.viewCount = s.viewCount + 1 WHERE s.performance.id = :performanceId")
    int incrementViewCount(@Param("performanceId") Long performanceId);

    /**
     * 分享数原子更新，返回值改为 int
     * @param performanceId
     * @return
     */
    @Modifying
    @Query("UPDATE PerformanceStats s SET s.shareCount = s.shareCount + 1 WHERE s.performance.id = :performanceId")
    int incrementShareCount(@Param("performanceId") Long performanceId);
}