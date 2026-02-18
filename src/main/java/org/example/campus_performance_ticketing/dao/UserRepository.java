package org.example.campus_performance_ticketing.dao;

import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
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
     * 根据用户名精确查询用户
     */
    Optional<UserInfo> findByNickname(String nickname);

    /**
     * 根据用户名模糊查询用户（同时匹配角色）
     */
    List<UserInfo> findByNicknameContainingAndRole(String nickname, String role);

    /**
     * 根据用户名模糊查询用户列表
     */
    List<UserInfo> findByNicknameContaining(String nickname);

    // 分页版模糊查询
    org.springframework.data.domain.Page<UserInfo> findByNicknameContaining(String nickname, org.springframework.data.domain.Pageable pageable);

    // 分页版模糊查询并按角色过滤
    org.springframework.data.domain.Page<UserInfo> findByNicknameContainingAndRole(String nickname, String role, org.springframework.data.domain.Pageable pageable);

    /**
     * 只返回公开字段 DTO
     */
    @Query("SELECT new org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo(u.nickname, u.avatar, u.major, u.college, u.status) " +
            "FROM UserInfo u WHERE u.id = :id")
    Optional<PublicUserInfo> findPublicUserInfoById(@Param("id") Long id);
    /**
     * 根据用户名查询公开信息
     */
    @Query("SELECT new org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo(u.nickname, u.avatar, u.major, u.college, u.status) " +
            "FROM UserInfo u WHERE u.nickname = :nickname")
    Optional<PublicUserInfo> findPublicUserInfoByNickname(@Param("nickname") String nickname);
}