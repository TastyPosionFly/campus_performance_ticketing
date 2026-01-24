package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.PerformanceStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceStaffRepository extends JpaRepository<PerformanceStaff, Long> {

    /**
     * 获取某演出的所有演职人员
     * 自动按 sortOrder (排序权重) 升序排列，保证导演、主演排在前面
     */
    List<PerformanceStaff> findByPerformanceIdOrderBySortOrderAsc(Long performanceId);
}