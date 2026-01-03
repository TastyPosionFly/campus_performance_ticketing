package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 组织相册照片表
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization_photo")
public class OrganizationPhoto {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 所属组织ID */
    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    /** 照片 URL */
    @Column(length = 255, nullable = false)
    private String url;

    /** 上传时间 */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
