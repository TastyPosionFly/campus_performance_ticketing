package org.example.campus_performance_ticketing.logic.dto.venue;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BlockVenueRequestDto {

    @NotNull(message = "场馆 ID 不能为空")
    private Long venueId;

    // 旧字段：单天屏蔽（兼容旧前端）
    private LocalDate blockedDate;

    // 新字段：批量屏蔽
    private List<LocalDate> blockedDates;

    // 一个理由，应用到所有日期
    private String reason;

    /**
     * 归一化：把 blockedDate / blockedDates 合成一个 dates 列表
     */
    public List<LocalDate> resolveBlockedDates() {
        List<LocalDate> dates = new ArrayList<>();
        if (blockedDates != null) {
            for (LocalDate d : blockedDates) {
                if (d != null) dates.add(d);
            }
        }
        if (blockedDate != null) dates.add(blockedDate);
        return dates;
    }
}