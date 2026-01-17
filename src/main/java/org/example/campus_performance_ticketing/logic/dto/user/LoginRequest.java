package org.example.campus_performance_ticketing.logic.dto.user;

import lombok.Data;

/**
 * 登录请求 DTO
 */

@Data
public class LoginRequest {

    /**
     * 微信 openid（唯一标识）
     */
    private String openid;

    /**
     * 昵称（可选）
     */
    private String nickname;

    /**
     * 头像 URL（可选）
     */
    private String avatar;
}
