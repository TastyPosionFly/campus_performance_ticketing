package org.example.campus_performance_ticketing.logic;

import org.example.campus_performance_ticketing.logic.dto.performance.PerformanceUpdateNotificationDto;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void sendNotification(PerformanceUpdateNotificationDto notification) {
        // 示例消息通知发送
        System.out.println("通知发送至用户 " + notification.getUserId() + ": " +
                "\n标题: " + notification.getTitle() +
                "\n内容: " + notification.getContent() +
                "\n延期原因: " + notification.getDelayReason());
    }
}