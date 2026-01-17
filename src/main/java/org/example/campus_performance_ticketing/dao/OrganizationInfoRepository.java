package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationInfoRepository extends JpaRepository<OrganizationInfo, Long> {
    // 可自定义查询方法

    /**
     * 根据状态查找组织列表
     * @param status
     * @return
     */
    List<OrganizationInfo> findByStatus(int status);

    /**
     * 删除指定状态的组织
     * @param status
     */
    void deleteByStatus(int status);

    /**
     * 根据负责人ID查找组织列表
     * @param leaderId
     * @return
     */
    List<OrganizationInfo> findAllByLeaderId(Long leaderId);
}
