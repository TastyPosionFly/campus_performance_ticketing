package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户基础信息实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_info")
public class UserInfo {

    /** 用户主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 微信 openid，用户唯一标识 */
    @Column(nullable = false, unique = true, length = 64)
    private String openid;

    /** 用户昵称 */
    @Column(length = 50)
    private String nickname;

    /** 用户头像 URL */
    @Column(length = 255)
    private String avatar;

    /**
     * 用户身份类型
     * 1-学生 2-学校职工 3-校外人员
     */
    @Column(name = "user_identity")
    private Integer userIdentity;

    /** 学生学号（仅学生） */
    @Column(name = "student_no", length = 30)
    private String studentNo;

    /** 学生专业 */
    @Column(length = 100)
    private String major;

    /** 学院 / 学校 */
    @Column(length = 100)
    private String college;

    /** 手机号 */
    @Column(length = 20)
    private String phone;

    /**
     * 系统角色
     * USER / ORGANIZER / VENUE_ADMIN / ADMIN / SUPER_ADMIN
     */
    @Column(length = 20)
    private String role;

    /**
     * 账号状态
     * 1-正常 0-封禁
     */
    private Integer status;

    /** 最后登录时间 */
    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    /** 创建记录时间 */
    @CreationTimestamp                    // Hibernate 特定注解
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /** 信息更新时间 */
    @UpdateTimestamp                      // Hibernate 特定注解
    @Column(name = "update_time")
    private LocalDateTime updateTime;

}
