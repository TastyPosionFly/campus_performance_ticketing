package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 用户票据实体
 * 对应表：ticket
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ticket", indexes = {
        @Index(name = "uk_ticket_code", columnList = "ticket_code", unique = true),
        @Index(name = "idx_ticket_user", columnList = "user_id"),
        // 联合索引：用于快速统计某场次、某状态下的票数
        @Index(name = "idx_perf_session_status", columnList = "performance_id, session_id, status")
})
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 唯一核销码
     */
    @Column(name = "ticket_code", nullable = false, unique = true, length = 64)
    private String ticketCode;

    /**
     * 关联用户
     * 对应 SQL: user_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserInfo user;

    /**
     * 关联演出
     * 对应 SQL: performance_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    /**
     * 关联场次
     * 对应 SQL: session_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PerformanceSession session;

    /**
     * 状态: 0-已预约 1-已核销 2-已失效 3-已取消
     */
    @Column(nullable = false)
    private Integer status;

    /**
     * 实际入场时间 (核销时间)
     */
    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    /**
     * 检票员 ID
     * 注：此处仅记录 ID 用于审计，通常无需关联整个 User 对象加载
     */
    @Column(name = "check_in_operator_id")
    private Long checkInOperatorId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}