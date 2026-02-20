package org.example.campus_performance_ticketing.logic.dto.performance;

import lombok.Data;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceSession;
import org.example.campus_performance_ticketing.model.PerformanceStaff;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class PerformanceDetailDto {
    private Long performanceId;
    private String title;
    private String description;
    private String posterUrl;
    private Integer publishStatus;
    private String statusDesc;
    private List<SessionCmd> sessions;
    private List<StaffCmd> staff;

    public static PerformanceDetailDto from(Performance performance) {
        PerformanceDetailDto dto = new PerformanceDetailDto();
        dto.setPerformanceId(performance.getId());
        dto.setTitle(performance.getTitle());
        dto.setDescription(performance.getDescription());
        dto.setPosterUrl(performance.getPosterUrl());
        dto.setPublishStatus(performance.getPublishStatus());
        dto.setSessions(performance.getSessions().stream().map(session -> {
            SessionCmd sessionCmd = new SessionCmd();
            sessionCmd.setVenueId(session.getVenue().getId());
            sessionCmd.setVenueName(session.getVenue().getName());
            sessionCmd.setStartTime(session.getStartTime());
            sessionCmd.setEndTime(session.getEndTime());
            sessionCmd.setTicketTotal(session.getTicketTotal());
            sessionCmd.setTicketSurplus(session.getTicketSurplus());
            return sessionCmd;
        }).collect(Collectors.toList()));
        dto.setStaff(performance.getStaffList().stream().map(staff -> {
            StaffCmd staffCmd = new StaffCmd();
            staffCmd.setStaffName(staff.getStaffName());
            staffCmd.setStaffType(staff.getStaffType());
            staffCmd.setStaffAvatar(staff.getStaffAvatar());
            staffCmd.setIntroduction(staff.getIntroduction());
            return staffCmd;
        }).collect(Collectors.toList()));
        return dto;
    }


    /**
     * 发布状态: 0-待审批, 1-已发布/未开演, 2-已下架, 3-已结束, 4-审批拒绝, 5-草稿, 6-被征用/需重排
     */
    public void setPublishStatus(Integer publishStatus) {
        this.publishStatus = publishStatus;
        switch (publishStatus) {
            case 0 -> this.statusDesc = "待审批";
            case 1 -> this.statusDesc = "未开演";
            case 2 -> this.statusDesc = "已下架";
            case 3 -> this.statusDesc = "已结束";
            case 4 -> this.statusDesc = "审批拒绝";
            case 5 -> this.statusDesc = "草稿";
            case 6 -> this.statusDesc = "被征用/需重排";
            default -> this.statusDesc = "未知状态";
        }
    }
}