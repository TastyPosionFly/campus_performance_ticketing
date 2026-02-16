package org.example.campus_performance_ticketing.logic.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录结果 DTO
 */

@Data
@AllArgsConstructor
public class LoginResult {
    private String token;
    private Long userId;
    private String openid;
    private String nickname;
    private String avatar;
    private String role;
    private int state;
    private boolean isOrgAdmin;
}
