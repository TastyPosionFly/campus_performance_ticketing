package org.example.campus_performance_ticketing.logic.dto.ticket;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 票据详情 DTO (用于展示)
 */
@Data
public class TicketDetailDTO {

    /** 票据 ID */
    private Long id;

    /** 核销码 (重要凭证) */
    private String ticketCode;

    /** 票据状态: 0-已预约 1-已核销 2-已取消 3-已失效 */
    private Integer status;

    /** 票据状态文本描述 */
    private String statusText;

    /** 演出标题 */
    private String performanceTitle;

    /** 演出海报 URL */
    private String performancePosterUrl;

    /** 场次 ID */
    private Long sessionId;

    /** 演出场地名称 */
    private String venueName;

    /** 场地地址 */
    private String venueAddress;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /**
     * 电子票背景图 URL (可选，如果有的话)
     * 后续查询详情时可能需要，预约成功时可能还没生成或不需要
     */
    private String ticketBgUrl;

    /** 预约时间 (创建时间) */
    private LocalDateTime bookingTime;

    public void setStatus(Integer status) {
        this.status = status;
        switch (status) {
            case 0 -> this.statusText = "已预约";
            case 1 -> this.statusText = "已核销";
            case 2 -> this.statusText = "已取消";
            case 3 -> this.statusText = "已失效";
            default -> this.statusText = "未知状态";
        }
    }
}