package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 演出人工推荐配置实体
 * 对应表：performance_recommendation
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "performance_recommendation", indexes = {
        @Index(name = "idx_rec_valid", columnList = "type, start_time, end_time")
},
        uniqueConstraints = {
                // 联合唯一约束：同一个演出在同一个 Type 下只能有一条记录
                @UniqueConstraint(name = "uk_perf_type", columnNames = {"performance_id", "type"})
})
public class PerformanceRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    /**
     * 推荐位置: 1-首页轮播 2-列表置顶
     */
    @Column(nullable = false)
    private Integer type;

    /**
     * 排序优先级
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 展示开始时间
     */
    @Column(name = "start_time")
    private LocalDateTime startTime;

    /**
     * 展示结束时间
     */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}