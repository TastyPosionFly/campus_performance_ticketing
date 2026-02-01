package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 演出数据聚合统计实体
 * 对应表：performance_stats
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "performance_stats", indexes = {
        @Index(name = "idx_stats_hot", columnList = "hot_score")
})
public class PerformanceStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false, unique = true)
    private Performance performance;

    @Column(name = "view_count")
    private Long viewCount = 0L;

    @Column(name = "share_count")
    private Long shareCount = 0L;

    @Column(name = "comment_count")
    private Long commentCount = 0L;

    /**
     * 已预约/售出票数 (来自 Ticket 表聚合)
     */
    @Column(name = "ticket_sold_count")
    private Integer ticketSoldCount = 0;

    /**
     * 实际核销/到场人数 (来自 Ticket 表聚合)
     */
    @Column(name = "ticket_check_in_count")
    private Integer ticketCheckInCount = 0;

    /**
     * 综合热度分
     */
    @Column(name = "hot_score")
    private Double hotScore = 0.0;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}