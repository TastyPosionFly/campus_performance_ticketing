package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户-组织关联关系
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "user_organization",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"})
)
public class UserOrganization {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 组织ID */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /**
     * 组织内角色
     * LEADER / MEMBER / PENDING
     */
    @Column(nullable = false, length = 20)
    private String role;

    /** 加入组织时间 */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
