package org.example.campus_performance_ticketing.logic.dto.venue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 请求屏蔽场馆的 DTO
 */
@Data
public class BlockVenueRequestDto {

    @NotNull(message = "场馆 ID 不能为空")
    private Long venueId;

    @NotNull(message = "屏蔽的日期不能为空")
    private LocalDate blockedDate;

    @NotBlank(message = "屏蔽原因不能为空")
    private String reason;
}