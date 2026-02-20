package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.OrganizationMember;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    List<OrganizationMember> findByOrganizationId(Long organizationId);
    List<OrganizationMember> findByUserId(Long userId);

    /**
     * 根据组织和用户查找组织成员
     * @param organization
     * @param user
     * @return
     */
    Optional<OrganizationMember> findByOrganizationAndUser(OrganizationInfo organization, UserInfo user);

    /**
     * 根据组织 ID 和用户 ID 查找组织成员
     * @param organizationId
     * @param userId
     * @return
     */
    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);

    /**
     * 删除指定状态的组织成员
     * @param status
     */
    void deleteByStatus(int status);

    /**
     * 根据组织 ID 和用户 ID 判断组织成员是否存在
     * @param organizationId
     * @param userId
     * @return
     */
    boolean existsByOrganizationIdAndUserId(Long organizationId, Long userId);


}
