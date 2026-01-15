// 可新建 ApplicationPublicDto
package org.example.campus_performance_ticketing.logic.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationPublicDto {
    private Long id;
    private PublicUserInfo applicant;
    private String applicationType;
    private Long targetId;
    private String extraData;
    private Integer status;
    private LocalDateTime applyTime;
    private LocalDateTime approveTime;
}