package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 演出媒体外链实体
 * 对应表：performance_media_link
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "performance_media_link", indexes = {
        @Index(name = "idx_media_pid", columnList = "performance_id")
})
public class PerformanceMediaLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    /**
     * 资源类型: 1-录像回放 2-在线直播
     */
    @Column(nullable = false)
    private Integer type;

    /**
     * 平台: 1-Bilibili 2-微信视频号 3-其他链接
     */
    @Column(nullable = false)
    private Integer platform;

    /**
     * 完整跳转链接(URL)
     */
    @Column(name = "external_key", nullable = false, length = 500)
    private String externalKey;

    /**
     * 标题
     */
    @Column(length = 100)
    private String title;

    /**
     * 排序权重 (大数在前或小数在前由业务决定，通常用作 ORDER BY sort_order)
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 目标小程序AppID(可选)
     */
    @Column(name = "app_id", length = 64)
    private String appId;

    /**
     * 目标小程序路径(可选)
     */
    @Column(length = 255)
    private String path;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}