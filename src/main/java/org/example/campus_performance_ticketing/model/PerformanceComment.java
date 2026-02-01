package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 演出评论实体
 * 对应表：performance_comment
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "performance_comment", indexes = {
        @Index(name = "idx_comment_pid", columnList = "performance_id"),
        @Index(name = "idx_comment_uid", columnList = "user_id")
})
public class PerformanceComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserInfo user;

    @Column(nullable = false, length = 1000)
    private String content;

    /**
     * 审核状态 (1-正常 0-隐藏)
     */
    @Column(columnDefinition = "tinyint default 1")
    private Integer status;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
