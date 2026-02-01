package org.example.campus_performance_ticketing.logic.dto.performance_recommendation;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateRecommendationCmd {

    @NotNull(message = "演出ID不能为空")
    private Long performanceId;

    /**
     * 推荐位置: 1-首页轮播 2-列表置顶
     */
    @NotNull(message = "推荐类型不能为空")
    private Integer type;

    /**
     * 排序权重 (数字越大越靠前)
     */
    private Integer sortOrder;

    /**
     * 开始展示时间 (空则立即开始)
     */
    private LocalDateTime startTime;

    /**
     * 结束展示时间 (空则永久展示)
     */
    private LocalDateTime endTime;
}