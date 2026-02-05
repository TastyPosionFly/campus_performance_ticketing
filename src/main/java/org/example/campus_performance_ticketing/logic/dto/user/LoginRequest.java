package org.example.campus_performance_ticketing.logic.dto.user;

import lombok.Data;

/**
 * 登录请求 DTO
 */

@Data
public class LoginRequest {

    /**
     * 微信 openid（唯一标识）
     * 前端可选提供，后端会通过 code 验证真实性
     */
    private String openid;

    /**
     * 微信登录临时凭证 code（必需）
     * 前端通过 wx.login() 获取
     */
    private String code;

    /**
     * 昵称（可选）
     */
    private String nickname;

    /**
     * 头像 URL（可选）
     */
    private String avatar;
}