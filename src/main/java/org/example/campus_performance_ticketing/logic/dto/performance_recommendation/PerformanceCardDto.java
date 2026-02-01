package org.example.campus_performance_ticketing.logic.dto.performance_recommendation;

import lombok.Data;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceStats;

import java.time.LocalDateTime;

/**
 * 演出卡片 DTO
 * 用途：首页推荐、热门列表、搜索结果列表
 * 特点：轻量级，包含热度数据，不包含场次和演职人员详情
 */
@Data
public class PerformanceCardDto {

    private Long id;
    private String title;
    private String description; // 简短描述
    private String posterUrl;

    // 分类信息 (建议在 Service 层填充)
    private Integer categoryId;
    // private String categoryName; // 如果你有 Category 缓存，可以在这里加上

    // 状态
    private Integer publishStatus;
    private String statusDesc;

    // 时间信息
    private LocalDateTime createTime;

    // === 统计/热度数据 (来自 PerformanceStats) ===
    private Long viewCount;
    private Long shareCount;
    private Long commentCount;
    private Double hotScore;

    // === 推荐理由 (可选，用于UI展示标签) ===
    // 例如："小编推荐", "热度飙升", "即将售罄"
    private String recommendationTag;

    /**
     * 基础转换方法 (仅转换 Performance 实体数据)
     */
    public static PerformanceCardDto from(Performance p) {
        PerformanceCardDto dto = new PerformanceCardDto();
        dto.setId(p.getId());
        dto.setTitle(p.getTitle());
        // 描述截断，防止列表页文字过多
        dto.setDescription(truncateDescription(p.getDescription()));
        dto.setPosterUrl(p.getPosterUrl());
        dto.setCategoryId(p.getCategoryId());
        dto.setCreateTime(p.getCreatedAt());

        dto.setPublishStatus(p.getPublishStatus());
        // 自动设置状态描述
        dto.setStatusDesc(getStatusDescription(p.getPublishStatus()));

        // 默认统计值为0，防止前端空指针
        dto.setViewCount(0L);
        dto.setShareCount(0L);
        dto.setCommentCount(0L);
        dto.setHotScore(0.0);

        return dto;
    }

    /**
     * 填充统计数据
     */
    public void fillStats(PerformanceStats stats) {
        if (stats != null) {
            this.viewCount = stats.getViewCount();
            this.shareCount = stats.getShareCount();
            this.commentCount = stats.getCommentCount();
            this.hotScore = stats.getHotScore();
        }
    }

    // --- 辅助方法 ---

    private static String truncateDescription(String desc) {
        if (desc == null) return "";
        return desc.length() > 50 ? desc.substring(0, 50) + "..." : desc;
    }

    private static String getStatusDescription(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待审批";
            case 1 -> "已发布";
            case 2 -> "已下架";
            case 3 -> "已结束";
            case 4 -> "审批拒绝";
            case 5 -> "草稿";
            case 6 -> "被征用";
            default -> "未知状态";
        };
    }
}