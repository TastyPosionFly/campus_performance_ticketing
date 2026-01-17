package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 统一申请审批实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "application")
public class Application {

    /** 申请主键 ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 申请人 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", referencedColumnName = "id", nullable = false)
    private UserInfo applicant;

    /**
     * 申请类型
     * CREATE_ORG/JOIN_ORG/DISBAND_ORG
     */
    @Column(name = "application_type", length = 40, nullable = false)
    private String applicationType;

    /**
     * 目标对象ID（可以是OrganizationInfo/OrganizationMember/OrganizationAlbum等的ID）
     * 为演示方便，这里只用Long，也可以根据业务需要做具体外键
     */
    @Column(name = "target_id")
    private Long targetId;

    /** 其他参数，灵活扩展 */
    @Column(name = "extra_data", columnDefinition = "json")
    private String extraData;

    /**
     * 申请状态
     * 1-待审核 2-通过 3-拒绝 4-撤销
     */
    @Column(nullable = false)
    private Integer status;

    /** 申请时间 */
    @CreationTimestamp
    @Column(name = "apply_time", updatable = false)
    private LocalDateTime applyTime;

    /** 审批时间 */
    @Column(name = "approve_time")
    private LocalDateTime approveTime;

    /** 审批人 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id", referencedColumnName = "id")
    private UserInfo approver;
}