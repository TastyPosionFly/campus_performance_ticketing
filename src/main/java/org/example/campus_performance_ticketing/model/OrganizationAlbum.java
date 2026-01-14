package org.example.campus_performance_ticketing.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 组织相册实体
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "organization_album")
public class OrganizationAlbum {

    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 照片 URL */
    @Column(name = "photo_url", length = 255, nullable = false)
    private String photoUrl;

    /** 组织 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", referencedColumnName = "id", nullable = false)
    private OrganizationInfo organization;

    /** 上传者 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", referencedColumnName = "id", nullable = false)
    private UserInfo uploader;

    /** 上传时间 */
    @CreationTimestamp
    @Column(name = "upload_time", updatable = false)
    private LocalDateTime uploadTime;

    /** 图片描述 */
    @Column(length = 255)
    private String description;
}