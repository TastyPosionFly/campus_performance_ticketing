package org.example.campus_performance_ticketing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 演出场次实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "performance_session")
public class PerformanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    @JsonIgnore // 防止 JSON 序列化死循环
    private Performance performance;

    /**
     * 关联场地实体
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "ticket_total")
    private Integer ticketTotal;

    @Column(name = "ticket_surplus")
    private Integer ticketSurplus;
}