package org.example.campus_performance_ticketing.logic.dto.performance;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** 延期申请命令 */
@Data
public  class DelaySessionCmd {
    @NotNull
    private Long sessionId;
    @NotNull @Future
    private LocalDateTime newStartTime;
    @NotNull @Future
    private LocalDateTime newEndTime;
    @NotBlank(message = "延期原因不能为空")
    private String reason;
}
