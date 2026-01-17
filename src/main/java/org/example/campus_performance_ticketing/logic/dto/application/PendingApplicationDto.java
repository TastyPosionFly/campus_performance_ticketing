package org.example.campus_performance_ticketing.logic.dto.application;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待处理的申请 DTO
 */

@Data
public class PendingApplicationDto {
    private Long applicationId; // 申请主键 ID
    private String applicationType; // 申请类型
    private String applicantOpenId; // 申请人用户唯一标识
    private String applicantName; // 申请人用户昵称
    private LocalDateTime applyTime; //申请时间

    private Integer status; // 申请状态 1-待审核 2-通过 3-拒绝 4-撤销
    private String statusDesc; // 状态文字描述

    // 通用：目标对象ID
    private Long targetId;

    // 通用：额外参数(JSON格式字符串)
    private String extraData;

    // 可选展示字段（可解析extraData后设置）
    private String displayTitle;
    private String displayDescription;

    public void setStatus(Integer status) {
        this.status = status;
        this.statusDesc = switch (status) {
            case 1 -> "待审核";
            case 2 -> "已通过";
            case 3 -> "已拒绝";
            case 4 -> "已撤销";
            default -> "未知状态";
        };
    }
}