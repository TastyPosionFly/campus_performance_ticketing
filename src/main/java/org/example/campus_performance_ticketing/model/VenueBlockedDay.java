package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 场地特殊日期屏蔽实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "venue_blocked_days", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"venue_id", "blocked_date"})
})
public class VenueBlockedDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关联场地 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    /** 被屏蔽的日期 (YYYY-MM-DD) */
    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;

    /** 屏蔽原因 */
    @Column(length = 255)
    private String reason;

    /** 操作人 (关联 UserInfo) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private UserInfo creator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}