package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationInfoRepository extends JpaRepository<OrganizationInfo, Long> {
    // 可自定义查询方法
}
