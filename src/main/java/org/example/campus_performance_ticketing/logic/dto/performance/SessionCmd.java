package org.example.campus_performance_ticketing.logic.dto.performance;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public  class SessionCmd {
    private Long sessionId; // 更新时需要，新增时不需要
    @NotNull
    private Long venueId;
    private String venueName;
    @NotNull @Future(message = "开始时间必须是将来") private LocalDateTime startTime;
    @NotNull @Future(message = "结束时间必须是将来") private LocalDateTime endTime;
    private Integer ticketTotal;
    private Integer ticketSurplus;
}
