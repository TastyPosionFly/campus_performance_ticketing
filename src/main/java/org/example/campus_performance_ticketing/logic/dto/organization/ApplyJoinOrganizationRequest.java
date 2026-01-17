package org.example.campus_performance_ticketing.logic.dto.organization;

import lombok.Data;

@Data
public class ApplyJoinOrganizationRequest {
    private Long orgId;
    private String reason;
}