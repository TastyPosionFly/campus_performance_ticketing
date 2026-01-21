package org.example.campus_performance_ticketing.logic.dto.venue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class UpdateVenueDto {

    /** 场地 ID (必填) */
    @NotNull(message = "场地ID不能为空")
    private Long id;

    // --- 基础信息 (可选) ---
    @Size(max = 50)
    private String name;

    @Size(max = 1000)
    private String description;

    private String address;
    private Integer capacity;
    private Integer type;
    private Integer status;

    // --- 封面图更新 (直接替换) ---
    private String coverImageUrl;
    private MultipartFile coverImageFile;

    // --- 轮播图更新策略 ---

    // 1. 纯新增
    private List<String> newPhotoUrlList;
    private List<MultipartFile> newPhotoFiles;

    // 2. 纯删除 (ID列表)
    private List<String> deletePhotoIds;

    /**
     * 替换指令 JSON。
     * 格式示例： {"uuid-old-1": 0, "uuid-old-2": 1}
     * 含义：把 ID 为 "uuid-old-1" 的图替换为 replaceFiles[0]
     *       把 ID 为 "uuid-old-2" 的图替换为 replaceFiles[1]
     */
    private String replacePhotoMap;

    /**
     * 用于替换的新文件列表 (顺序对应 replacePhotoMap 中的 value 索引)
     */
    private List<MultipartFile> replaceFiles;

    // --- 其他 ---
    private String equipmentInfo;
    private Long managerId;
}