package org.example.campus_performance_ticketing.logic.dto.organization;

import lombok.Data;

@Data
public class ChangeMemberRoleRequest {
    private Long orgId;
    private Long memberId;
    private String newRole;
}
