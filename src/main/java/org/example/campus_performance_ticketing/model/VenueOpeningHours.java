package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 场地开放时间配置实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "venue_opening_hours", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"venue_id", "day_of_week"})
})
public class VenueOpeningHours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联场地 (多对一) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    /** 星期(1=周一, 7=周日) */
    @Column(name = "day_of_week", nullable = false, columnDefinition = "tinyint unsigned")
    private Integer dayOfWeek;

    /** 开始营业时间 */
    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    /** 结束营业时间 */
    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    /** 当天是否休息(1:休息, 0:开放) */
    @Column(name = "is_closed", columnDefinition = "tinyint(1) default 0")
    private Boolean isClosed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}