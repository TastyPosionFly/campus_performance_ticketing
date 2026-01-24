package org.example.campus_performance_ticketing.logic.dto.performance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public  class StaffCmd {
    private Long userId; // 可选，系统用户ID

    @NotBlank
    private String staffName;

    @NotBlank
    private String staffType; // 职位：导演/演员

    private String staffAvatar;
    private String introduction;
    private Integer sortOrder;
}
