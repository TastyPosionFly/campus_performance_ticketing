package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.PerformanceComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerformanceCommentRepository extends JpaRepository<PerformanceComment, Long> {

    /**
     * 分页查询某场演出的正常评论（status=1），按时间倒序
     */
    Page<PerformanceComment> findByPerformanceIdAndStatusOrderByCreateTimeDesc(Long performanceId, Integer status, Pageable pageable);

    /**
     * 统计某场演出的评论数
     */
    long countByPerformanceIdAndStatus(Long performanceId, Integer status);
}