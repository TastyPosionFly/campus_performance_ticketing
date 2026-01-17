package org.example.campus_performance_ticketing.logic.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 公开的用户信息 DTO
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserInfo {
    private Long userId;
    private String nickname;
    private String avatar;
    private String major;
    private String college;

    private Integer status;
    private String statusDesc; // 状态文字描述

    public PublicUserInfo(String nickname, String avatar, String major, String college, Integer status) {
        this.nickname = nickname;
        this.avatar = avatar;
        this.major = major;
        this.college = college;
        this.setStatus(status);
    }

    public void setStatus(Integer status) {
        this.status = status;
        this.statusDesc = switch (status) {
            case 1 -> "正常用户";
            case 0 -> "封禁用户";
            default -> "未知状态";
        };
    }


}
