package org.example.campus_performance_ticketing.logic.dto.venue;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 开放时间和屏蔽时间的联合 DTO
 */
@Data
@AllArgsConstructor
public class OpeningAndBlockedHoursDto {

    private List<OpeningHoursDto> openingHours; // 每天开放时间
    private List<LocalDate> blockedDates;       // 屏蔽日期
}