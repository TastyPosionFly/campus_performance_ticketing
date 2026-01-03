package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 演出组织 / 活动主办方
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization")
public class Organization {

    /** 组织主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 组织名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 组织简介 */
    @Column(length = 255)
    private String description;

    /** 组织照片 URL */
    @Column(length = 255)
    private String avatar;

    /** 负责人用户ID */
    @Column(name = "leader_user_id", nullable = false)
    private Long leaderUserId;

    /**
     * 组织状态
     * 0-待审核 1-正常 2-停用
     */
    private Integer status;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
