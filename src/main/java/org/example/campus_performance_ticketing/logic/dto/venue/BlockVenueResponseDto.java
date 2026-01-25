package org.example.campus_performance_ticketing.logic.dto.venue;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * 返回屏蔽场馆操作结果的 DTO
 */
@Data
@AllArgsConstructor
public class BlockVenueResponseDto {
    private Long venueId;          // 场馆 ID
    private LocalDate blockedDate; // 被屏蔽的日期
    private String reason;         // 屏蔽原因
    private int canceledPerformancesCount; // 已取消的演出数量
    private String message;        // 操作结果消息
}