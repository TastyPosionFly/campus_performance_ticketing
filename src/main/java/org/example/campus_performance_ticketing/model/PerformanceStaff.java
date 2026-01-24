package org.example.campus_performance_ticketing.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 演职人员实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "performance_staff")
public class PerformanceStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    @JsonIgnore
    private Performance performance;

    /**
     * 关联系统用户（可选，如果是校内人员）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserInfo user;

    @Column(name = "staff_name", nullable = false, length = 64)
    private String staffName;

    @Column(name = "staff_type", nullable = false, length = 64)
    private String staffType;

    @Column(name = "staff_avatar", length = 512)
    private String staffAvatar;

    @Column(length = 512)
    private String introduction;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}