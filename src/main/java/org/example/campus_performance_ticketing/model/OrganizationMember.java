package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 组织成员实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization_member")
public class OrganizationMember {

    /** 主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 组织 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
    private OrganizationInfo organization;

    /** 用户 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserInfo user;

    /**
     * 角色
     * MEMBER/LEADER/MANAGER
     */
    @Column(name = "member_role", length = 20)
    private String memberRole;

    /** 加入时间 */
    @CreationTimestamp
    @Column(name = "join_time", updatable = false)
    private LocalDateTime joinTime;

    /**
     * 状态
     * 1-在组织 2-已退出 3-被踢出
     */
    @Column(nullable = false)
    private Integer status;
}