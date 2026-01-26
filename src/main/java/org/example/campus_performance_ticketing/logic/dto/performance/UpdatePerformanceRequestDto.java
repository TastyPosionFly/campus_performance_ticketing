package org.example.campus_performance_ticketing.logic.dto.performance;

import jakarta.validation.Valid;
import lombok.Data;
import java.util.List;

@Data
public class UpdatePerformanceRequestDto {
    @Valid
    private UpdatePerformanceCmd performanceCmd; // 包含演出内容修改的命令对象

    @Valid
    private List<SessionCmd> sessions;          // 修改后的场次列表

    @Valid
    private List<StaffCmd> staffList;           // 演职人员信息

    private String delayReason;                 // 延期原因
}