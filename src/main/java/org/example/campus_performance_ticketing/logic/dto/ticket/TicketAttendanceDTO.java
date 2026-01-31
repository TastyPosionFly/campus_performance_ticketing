package org.example.campus_performance_ticketing.logic.dto.ticket;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class TicketAttendanceDTO extends PublicUserInfo {

    /** 学号 (仅学生或教职工可能有) */
    private String studentNo;

    /** 用户身份类型 */
    private Integer userIdentity;

    /** 用户身份描述 */
    private String userIdentityDesc;

    /** 实际入场时间 (核销时间) */
    private LocalDateTime checkInTime;

    // 构造函数，方便从 Entity 转换
    public TicketAttendanceDTO() {
        super();
    }

    public void setUserIdentity(Integer userIdentity) {
        this.userIdentity = userIdentity;
        this.userIdentityDesc = switch (userIdentity) {
            case 1 -> "学生";
            case 2 -> "学校职工";
            case 3 -> "校外人员";
            default -> "未知身份";
        };
    }
}