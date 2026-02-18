package org.example.campus_performance_ticketing.logic.dto.venue;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 场地日历事件 DTO（轻量）
 * 返回给前端用于日历展示：场次ID、演出ID、演出名称、举办者、开始/结束时间、演出日期
 */
@Data
public class VenueEventDto {
    private Long sessionId;         // PerformanceSession.id
    private Long performanceId;     // Performance.id
    private String performanceName; // Performance.title
    private String organizerName;   // 举办者（演出组织/用户名）
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDate performanceDate; // 通常取 startTime 的日期部分
}