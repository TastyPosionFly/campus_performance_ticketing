package org.example.campus_performance_ticketing.logic.dto.venue; // 建议放在单独的包

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 存储在 JSON 字段中的图片信息结构
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenuePhotoInfo implements Serializable {
    /** 图片唯一ID (用于前端精准删除/修改) */
    private String id;

    /** 图片存储路径/URL */
    private String url;

    /** 原始文件名 (可选，方便管理) */
    private String originalName;
}