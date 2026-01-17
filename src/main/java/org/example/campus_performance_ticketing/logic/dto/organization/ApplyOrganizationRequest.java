package org.example.campus_performance_ticketing.logic.dto.organization;

import lombok.Data;

/**
 * 申请创建组织请求 DTO
 */

@Data
public class ApplyOrganizationRequest {
    private String orgName;
    private String orgDescription;
    private String avatarUrl;

    // 组织头像文件
    private String avatarFile;
}