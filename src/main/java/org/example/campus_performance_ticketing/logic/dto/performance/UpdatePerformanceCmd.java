package org.example.campus_performance_ticketing.logic.dto.performance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 演出信息修改命令 (简化版) */
@Data
public class UpdatePerformanceCmd {
    @NotNull
    private Long performanceId;
    private String title;
    private String description;
    private String posterUrl;
    // 暂不包含场次修改，场次修改逻辑较复杂建议单独接口
}
