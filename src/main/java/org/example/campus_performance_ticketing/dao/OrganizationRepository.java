package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationRepository
        extends JpaRepository<Organization, Long> {

    /**
     * 查询负责人创建的所有组织
     */
    List<Organization> findByLeaderUserId(Long leaderUserId);

    /**
     * 查询状态正常的组织
     */
    List<Organization> findByStatus(Integer status);
}
