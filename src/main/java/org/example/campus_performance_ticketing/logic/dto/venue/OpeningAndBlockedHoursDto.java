package org.example.campus_performance_ticketing.logic.dto.venue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 场馆开放时间 + 屏蔽日期（屏蔽项只包含日期与原因）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpeningAndBlockedHoursDto {
    private List<OpeningHoursDto> openingHours; // 你已有的 OpeningHoursDto 列表
    private List<BlockedDateSimple> blockedDates; // 每条仅包含 date + reason
}