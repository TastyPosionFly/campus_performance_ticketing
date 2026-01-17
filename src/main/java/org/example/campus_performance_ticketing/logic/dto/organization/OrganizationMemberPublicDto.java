package org.example.campus_performance_ticketing.logic.dto.organization;

import lombok.Data;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;

/**
 * 组织成员公开信息 DTO
 */

@Data
public class OrganizationMemberPublicDto {
    private PublicUserInfo user;
    private String memberRole;

    private Integer status;
    private String statusDesc; // 状态文字描述

    public void setStatus(Integer status) {
        this.status = status;
        this.statusDesc = switch (status) {
            case 1 -> "已加入";
            case 2 -> "已退出";
            case 3 -> "已踢出";
            default -> "未知状态";
        };
    }
}