package org.example.campus_performance_ticketing.logic.dto.application;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 单条申请审核指令 DTO
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationAuditCommand {
    private Long applicationId;
    private Integer newStatus; // 2-同意，3-拒绝
    private String reason;
}