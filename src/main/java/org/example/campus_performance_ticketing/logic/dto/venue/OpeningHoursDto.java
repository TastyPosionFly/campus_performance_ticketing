package org.example.campus_performance_ticketing.logic.dto.venue;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpeningHoursDto {

    /** 星期几 (1-7) */
    @NotNull(message = "星期必须指定")
    @Min(value = 1, message = "星期最小为1")
    @Max(value = 7, message = "星期最大为7")
    private Integer dayOfWeek;

    /** 是否休息 */
    private Boolean isClosed;

    /**
     * 开始时间 (格式 HH:mm:ss)
     * 示例: "09:00:00"
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime openTime;

    /**
     * 结束时间 (格式 HH:mm:ss)
     * 示例: "22:00:00"
     */
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime closeTime;
}