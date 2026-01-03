package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.UserOrganization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserOrganizationRepository
        extends JpaRepository<UserOrganization, Long> {

    /**
     * 判断用户是否属于某组织
     */
    boolean existsByUserIdAndOrganizationId(Long userId, Long organizationId);

    /**
     * 查询用户在组织中的角色
     */
    Optional<UserOrganization> findByUserIdAndOrganizationId(Long userId, Long organizationId);

    /**
     * 获取组织中所有用户信息
     */
    List<UserOrganization> findAllByOrganizationId(Long organizationId);
}
