package org.example.campus_performance_ticketing.logic.dto.performance_recommendation;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PerformanceRecommendationAdminDTO {
    private Long id;
    private Integer type;
    private Integer sortOrder;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;

    private Long performanceId;
    private String performanceTitle;
    private String performancePosterUrl;
}