package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.PerformanceMediaLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceMediaLinkRepository extends JpaRepository<PerformanceMediaLink, Long> {

    /**
     * 查询某场演出的所有媒体资源，按排序权重降序排列
     */
    List<PerformanceMediaLink> findByPerformanceIdOrderBySortOrderDesc(Long performanceId);

    /**
     * 查询某场演出的特定类型资源 (例如只查直播)
     */
    List<PerformanceMediaLink> findByPerformanceIdAndType(Long performanceId, Integer type);
}