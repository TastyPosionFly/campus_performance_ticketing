package org.example.campus_performance_ticketing.logic;

public interface NotificationService {
    /**
     * 发送通知
     * @param userId 接收人 ID
     * @param title 标题
     * @param content 内容
     */
    void sendNotification(Long userId, String title, String content);
}