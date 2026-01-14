package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.logic.dto.PublicUserInfo;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<UserInfo, Long> {

    /**
     * 根据 openid 查询用户
     */
    Optional<UserInfo> findByOpenid(String openid);

    /**
     * 判断用户是否已注册
     */
    boolean existsByOpenid(String openid);

    /**
     * 只返回公开字段DTO
     */
    @Query("SELECT new org.example.campus_performance_ticketing.logic.dto.PublicUserInfo(u.nickname, u.avatar, u.major, u.college, u.status) " +
            "FROM UserInfo u WHERE u.openid = :openid")
    Optional<PublicUserInfo> findPublicUserInfoByOpenid(@Param("openid") String openid);

    // 根据组织ID获取该组织所有成员的详细信息
    @Query("SELECT u FROM UserInfo u WHERE u.id IN " +
            "(SELECT uo.userId FROM UserOrganization uo WHERE uo.organizationId = :orgId)")
    List<UserInfo> findAllUsersByOrganizationId(@Param("orgId") Long organizationId);
}

