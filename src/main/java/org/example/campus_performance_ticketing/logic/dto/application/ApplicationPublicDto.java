// 可新建 ApplicationPublicDto
package org.example.campus_performance_ticketing.logic.dto.application;

import lombok.Data;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;

import java.time.LocalDateTime;

/**
 * 公开的申请信息 DTO
 */

@Data
public class ApplicationPublicDto {
    private Long id;
    private PublicUserInfo applicant;
    private String applicationType;
    private Long targetId;
    private String extraData;

    private Integer status;
    private String statusDesc;  //1-待审核 2-通过 3-拒绝 4-撤销

    private LocalDateTime applyTime;
    private LocalDateTime approveTime;

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