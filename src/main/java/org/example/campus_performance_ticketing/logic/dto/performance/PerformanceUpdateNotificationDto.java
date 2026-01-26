package org.example.campus_performance_ticketing.logic.dto.performance;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PerformanceUpdateNotificationDto {
    private Long userId;       // 用户 ID
    private String title;      // 通知标题
    private String content;    // 通知内容
    private String delayReason; // 延期或修改原因
}