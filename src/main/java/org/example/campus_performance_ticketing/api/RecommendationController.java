package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.logic.PerformanceRecommendationService;
import org.example.campus_performance_ticketing.logic.PerformanceStatsService;
import org.example.campus_performance_ticketing.logic.RecommendationAggregationService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance_recommendation.CreateRecommendationCmd;
import org.example.campus_performance_ticketing.logic.dto.performance_recommendation.PerformanceCardDto;
import org.example.campus_performance_ticketing.model.PerformanceRecommendation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 推荐与热度统计统一接口
 * 包含：
 * 1. 首页/列表混合推荐 (AggregationService)
 * 2. 人工推荐位管理 (RecommendationService - Admin)
 * 3. 浏览/分享数据上报 (StatsService)
 */
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
@Validated
public class RecommendationController {

    private final RecommendationAggregationService aggregationService;
    private final PerformanceRecommendationService recommendationService;
    private final PerformanceStatsService statsService;

    // ==========================================
    // 1. 公共端接口
    // ==========================================

    /**
     * 获取混合推荐列表
     * 返回 PerformanceCardDto (轻量级卡片)
     */
    @GetMapping("/list")
    public ApiResponse<List<PerformanceCardDto>> getMixedList(
            @RequestParam @NotNull Integer type,
            @RequestParam(defaultValue = "10") int limit) {
        return aggregationService.getMixedRecommendationList(type, limit);
    }

    /**
     * 上报浏览量 (埋点)
     * 场景：用户点击进入演出详情页时调用
     */
    @PostMapping("/stats/view/{performanceId}")
    public ApiResponse<Void> reportView(@PathVariable Long performanceId) {
        statsService.incrementViewCount(performanceId);
        return ApiResponse.success(null);
    }

    /**
     * 上报分享数 (埋点)
     * 场景：用户点击分享按钮时调用
     */
    @PostMapping("/stats/share/{performanceId}")
    public ApiResponse<Void> reportShare(@PathVariable Long performanceId) {
        statsService.incrementShareCount(performanceId);
        return ApiResponse.success(null);
    }

    // ==========================================
    // 2. 管理端接口 (需要管理员权限)
    // ==========================================

    /**
     * 添加人工推荐位
     */
    @PostMapping("/admin/add")
    public ApiResponse<Void> addRecommendation(
            HttpServletRequest request,
            @RequestBody @Valid CreateRecommendationCmd cmd) {
        String openId = (String) request.getAttribute("openid");
        return recommendationService.addRecommendation(openId, cmd);
    }

    /**
     * 删除/下架人工推荐位
     */
    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> deleteRecommendation(
            HttpServletRequest request,
            @PathVariable Long id) {
        String openId = (String) request.getAttribute("openid");
        return recommendationService.deleteRecommendation(openId, id);
    }

    /**
     * 获取所有推荐配置 (后台列表管理用)
     * @param type 推荐类型
     */
    @GetMapping("/admin/config/list")
    public ApiResponse<List<PerformanceRecommendation>> getAllConfigs(
            HttpServletRequest request,
            @RequestParam @NotNull Integer type) {
        String openId = (String) request.getAttribute("openid");
        return recommendationService.getAllRecommendationsConfig(openId, type);
    }
}