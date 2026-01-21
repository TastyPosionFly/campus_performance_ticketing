package org.example.campus_performance_ticketing.logic.dto.venue;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 场地管理员精简信息
 * 用于对外展示，屏蔽 OpenID 等敏感数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenueManagerDto {

    private Long id;

    /** 显示名称 (对应 UserInfo 的 nickname) */
    private String name;

    /** 头像 URL (对应 UserInfo 的 avatarUrl) */
    private String avatarUrl;

    /** 联系电话 */
    private String phone;
}