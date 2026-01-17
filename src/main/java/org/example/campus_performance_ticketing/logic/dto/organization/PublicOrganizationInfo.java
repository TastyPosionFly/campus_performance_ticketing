package org.example.campus_performance_ticketing.logic.dto.organization;

import lombok.Data;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;

/**
 * 公开的组织信息 DTO
 */

@Data
public class PublicOrganizationInfo {
    private Long id;
    private String name;
    private String description;
    private String avatarUrl;
    private PublicUserInfo Leader;

    private Integer status;
    private String statusDesc;

    public void setStatus(Integer status) {
        this.status = status;
        this.statusDesc = switch (status) {
            case 1 -> "正常";
            case 3 -> "已解散";
            default -> "未知状态";
        };
    }
}