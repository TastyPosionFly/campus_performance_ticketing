package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 场地基础信息实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "venues")
// 1. 查询时自动过滤：替代旧版的 @Where
@SQLRestriction("deleted_at IS NULL")
// 2. 删除时自动转为更新：替代默认的 DELETE 语句
@SQLDelete(sql = "UPDATE venues SET deleted_at = NOW() WHERE id = ?")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /** 场地名称 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 场地描述/介绍 */
    @Column(columnDefinition = "text")
    private String description;

    /** 详细地址 */
    @Column(nullable = false)
    private String address;

    /** 封面图片URL */
    @Column(name = "cover_image")
    private String coverImage;

    /** 场地详情轮播图 (JSON Array) */
    @Column(name = "photo_list", columnDefinition = "json")
    private String photoList;

    /** 容纳人数 */
    @Column(columnDefinition = "int unsigned default 0")
    private Integer capacity;

    /** 场地类型 */
    @Column(columnDefinition = "tinyint unsigned default 1")
    private Integer type;

    /** 设备配置 (JSON Object) */
    @Column(name = "equipment_info", columnDefinition = "json")
    private String equipmentInfo;

    /** 场地状态 */
    @Column(columnDefinition = "tinyint unsigned default 1")
    private Integer status;

    /** 场地管理员 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", referencedColumnName = "id")
    private UserInfo manager;

    /** 创建人 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private UserInfo creator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 软删除时间 */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** 关联开放时间 */
    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VenueOpeningHours> openingHoursList;
}