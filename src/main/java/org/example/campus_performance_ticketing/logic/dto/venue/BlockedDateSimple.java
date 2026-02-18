package org.example.campus_performance_ticketing.logic.dto.venue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 简化的屏蔽日期 DTO：只包含日期和原因
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlockedDateSimple {
    private LocalDate date;
    private String reason;
}