package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.Performance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PerformanceRepository extends JpaRepository<Performance, Long>, JpaSpecificationExecutor<Performance> {

    /**
     * 根据发布状态查询演出列表
     * 用途：首页展示所有“已上架”的演出 (status=1)
     */
    List<Performance> findByPublishStatusOrderByCreatedAtDesc(Integer publishStatus);

    /**
     * 查询某个人或组织申请的所有演出
     * 用途：用户中心 -> "我的演出"列表
     * @param organizerType "USER" 或 "ORGANIZATION"
     * @param organizerId 用户ID 或 组织ID
     */
    List<Performance> findByOrganizerTypeAndOrganizerIdOrderByCreatedAtDesc(String organizerType, Long organizerId);

    /**
     * 根据分类查询已发布的演出
     * 用途：分类筛选功能
     */
    List<Performance> findByCategoryIdAndPublishStatus(Integer categoryId, Integer publishStatus);
}