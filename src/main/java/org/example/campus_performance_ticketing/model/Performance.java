package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 演出基础信息实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "performance")
public class Performance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "poster_url")
    private String posterUrl;

    @Column(name = "category_id")
    private Integer categoryId;

    /**
     * 举办方类型：USER / ORGANIZATION
     */
    @Column(name = "organizer_type", nullable = false, length = 20)
    private String organizerType;

    /**
     * 举办方ID (对应 UserInfo.id 或 Organization.id)
     * 注：因为是多态关联，无法直接映射为单一 Entity，保留 ID 字段
     */
    @Column(name = "organizer_id", nullable = false)
    private Long organizerId;

    /**
     * 发布状态: 0-待审批, 1-已发布, 2-已下架, 3-已结束
     */
    @Column(name = "publish_status")
    private Integer publishStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // --- 级联关系 ---

    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "performance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PerformanceStaff> staffList = new ArrayList<>();
}