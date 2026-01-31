package org.example.campus_performance_ticketing.logic.dto.ticket;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TicketBookingDTO {

    @NotNull(message = "场次 ID 不能为空")
    private Long sessionId;

}