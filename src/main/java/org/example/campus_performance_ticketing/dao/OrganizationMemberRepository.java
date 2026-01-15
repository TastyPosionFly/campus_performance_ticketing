package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.model.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    List<OrganizationMember> findByOrganizationId(Long organizationId);
    List<OrganizationMember> findByUserId(Long userId);


}
