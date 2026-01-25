package org.example.campus_performance_ticketing.logic.dto.performance;

import lombok.Data;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceSession;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理员创建/征用演出后的返回结果 DTO
 */
@Data
public class AdminPerformanceDto {

    private Long performanceId;
    private String title;
    private String organizerType;
    private Long organizerId;
    private Integer publishStatus;

    // 包含简单的场次列表
    private List<SessionSimpleDto> sessions;

    // 静态工厂方法：将实体转为 DTO
    public static AdminPerformanceDto from(Performance performance) {
        AdminPerformanceDto dto = new AdminPerformanceDto();
        dto.setPerformanceId(performance.getId());
        dto.setTitle(performance.getTitle());
        dto.setOrganizerType(performance.getOrganizerType());
        dto.setOrganizerId(performance.getOrganizerId());
        dto.setPublishStatus(performance.getPublishStatus());

        if (performance.getSessions() != null) {
            dto.setSessions(performance.getSessions().stream()
                    .map(SessionSimpleDto::from)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    /**
     * 内部类：简化的场次 DTO
     */
    @Data
    public static class SessionSimpleDto {
        private Long sessionId;
        private Long venueId;
        private String venueName; // 方便前端显示
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer ticketTotal;

        public static SessionSimpleDto from(PerformanceSession session) {
            SessionSimpleDto dto = new SessionSimpleDto();
            dto.setSessionId(session.getId());
            dto.setVenueId(session.getVenue().getId());
            dto.setVenueName(session.getVenue().getName());
            dto.setStartTime(session.getStartTime());
            dto.setEndTime(session.getEndTime());
            dto.setTicketTotal(session.getTicketTotal());
            return dto;
        }
    }
}