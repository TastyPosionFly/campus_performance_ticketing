package org.example.campus_performance_ticketing.logic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

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
}
