package org.example.campus_performance_ticketing.logic.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicUserInfo {
    private String nickname;
    private String avatar;
    private String major;
    private String college;
    private Integer status;
}
