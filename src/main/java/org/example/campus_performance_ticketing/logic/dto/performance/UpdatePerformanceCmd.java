package org.example.campus_performance_ticketing.logic.dto.performance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdatePerformanceCmd {
    @NotNull
    private Long performanceId;
    private String title;       // 修改标题
    private String description; // 修改描述
    private String posterUrl;   // 修改海报 URL
    private Integer publishStatus; // 发布状态（如上架 1 或下架 2）
}