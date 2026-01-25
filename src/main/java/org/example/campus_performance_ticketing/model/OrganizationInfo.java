package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 组织信息实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization_info")
public class OrganizationInfo {

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

    /** 组织头像 URL */
    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    /**
     * 组织首领用户
     * 外键关联 user_info.id
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "leader_id", referencedColumnName = "id", nullable = false)
    private UserInfo leader;

    /**
     * 组织状态
     * 1-正常 2-已解散
     */
    @Column(nullable = false)
    private Integer status;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}