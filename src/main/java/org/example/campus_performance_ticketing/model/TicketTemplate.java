package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 电子票模板实体
 * 对应表：ticket_template
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ticket_template", indexes = {
        @Index(name = "idx_template_session", columnList = "session_id")
})
public class TicketTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联场次
     * 对应 SQL: session_id
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private PerformanceSession session;

    /**
     * 电子票背景图 URL
     */
    @Column(name = "background_img_url")
    private String backgroundImgUrl;

    /**
     * 状态: 0-下架 1-上架
     */
    @Column(nullable = false)
    private Integer status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}