package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.*;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.CreatePerformanceCmd;
import org.example.campus_performance_ticketing.logic.dto.performance.SessionCmd;
import org.example.campus_performance_ticketing.logic.dto.performance.StaffCmd;
import org.example.campus_performance_ticketing.model.*;
import org.example.campus_performance_ticketing.logic.NotificationService;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceSessionRepository sessionRepository;
    private final VenueRepository venueRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;


    // === 注入文件目录配置 ===
    @Value("${performance.post.temp-dir}")
    private String posterTempDir;

    @Value("${staff.photo.temp-dir}")
    private String staffTempDir;

    @Value("${performance.post.upload-dir}")
    private String posterRealDir;

    @Value("${staff.photo.upload-dir}")
    private String staffRealDir;


    // ==========================================
    // A. 文件上传 (Controller 调用此层)
    // ==========================================
    public String uploadTempImage(MultipartFile file, String type) {
        try {
            if ("POSTER".equalsIgnoreCase(type)) {
                return FileUtil.saveImage(file, posterTempDir);
            } else if ("STAFF".equalsIgnoreCase(type)) {
                return FileUtil.saveImage(file, staffTempDir);
            } else {
                throw new IllegalArgumentException("未知的文件类型");
            }
        } catch (IOException e) {
            log.error("上传临时文件失败", e);
            throw new RuntimeException("文件上传失败");
        }
    }


    // ==========================================
    // B. 提交申请 (逻辑保持不变，存的是临时路径)
    // ==========================================
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> submitPerformanceApplication(String userOpenId, CreatePerformanceCmd cmd) {
        UserInfo applicant = userRepository.findByOpenid(userOpenId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        validateOrganizerAuthority(applicant, cmd.getOrganizerType(), cmd.getOrganizerId());

        // 存入数据库的是临时路径 (e.g., ./data/temp/poster/xxx.jpg)
        Performance performance = savePerformanceData(cmd, 0);

        createApplicationRecord(applicant, performance, cmd);

        log.info("申请提交成功，ID: {}", performance.getId());

        ApiResponse<Void> response = ApiResponse.success(null);
        response.setMessage("申请提交成功，等待审核");

        return response;
    }


    // ==========================================
    // C. 审批通过后的逻辑 (ApplicationTxService 调用此层)
    // 核心：移动文件 + 更新路径 + 上架
    // ==========================================
    @Transactional(rollbackFor = Exception.class)
    public void approveAndActivate(Long performanceId) {
        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("演出不存在"));

        // 1. 处理海报：从 temp -> real
        if (isTempPath(performance.getPosterUrl())) {
            try {
                String newPath = FileUtil.moveFile(performance.getPosterUrl(), posterRealDir);
                performance.setPosterUrl(newPath);
            } catch (IOException e) {
                log.error("移动海报文件失败: {}", performance.getPosterUrl(), e);
                // 即使移动失败，也可以选择继续，或者抛异常回滚。
                // 这里选择抛异常回滚，保证数据一致性
                throw new RuntimeException("审批失败：海报文件移动异常");
            }
        }

        // 2. 处理演职人员照片
        List<PerformanceStaff> staffList = performance.getStaffList();
        if (staffList != null) {
            for (PerformanceStaff staff : staffList) {
                if (isTempPath(staff.getStaffAvatar())) {
                    try {
                        String newStaffPath = FileUtil.moveFile(staff.getStaffAvatar(), staffRealDir);
                        staff.setStaffAvatar(newStaffPath);
                        // 注意：因为配置了 CascadeType.ALL，performance保存时会自动更新staff
                    } catch (IOException e) {
                        log.error("移动人员照片失败: {}", staff.getStaffAvatar(), e);
                        // 这里可以选择忽略单张照片的失败，或者抛异常
                    }
                }
            }
        }

        // 3. 更新状态为已上架
        performance.setPublishStatus(1);

        // 4. 保存更新后的路径和状态
        performanceRepository.saveAndFlush(performance);

        // 5. 发通知
        // notificationService.sendNotification(performance.getOrganizerId(), ...);
    }

    // 判断是否是临时文件
    private boolean isTempPath(String path) {
        return path != null && path.contains("/temp/");
    }

    // ... (保留之前的 savePerformanceData, checkVenueConflict, validateOrganizerAuthority 等辅助方法) ...
    // 为节省篇幅，此处省略私有辅助方法，请确保它们存在于类中

    private Performance savePerformanceData(CreatePerformanceCmd cmd, Integer status) {
        Performance performance = new Performance();
        performance.setTitle(cmd.getTitle());
        performance.setDescription(cmd.getDescription());
        performance.setPosterUrl(cmd.getPosterUrl());
        performance.setCategoryId(cmd.getCategoryId());
        performance.setOrganizerType(cmd.getOrganizerType());
        performance.setOrganizerId(cmd.getOrganizerId());
        performance.setPublishStatus(status);

        List<PerformanceSession> sessions = new ArrayList<>();
        if (cmd.getSessions() != null) {
            for (SessionCmd sessionCmd : cmd.getSessions()) {
                checkVenueConflict(sessionCmd.getVenueId(), sessionCmd.getStartTime(), sessionCmd.getEndTime(), null);
                Venue venue = venueRepository.findById(sessionCmd.getVenueId())
                        .orElseThrow(() -> new IllegalArgumentException("场地ID不存在: " + sessionCmd.getVenueId()));

                PerformanceSession session = new PerformanceSession();
                session.setPerformance(performance);
                session.setVenue(venue);
                session.setStartTime(sessionCmd.getStartTime());
                session.setEndTime(sessionCmd.getEndTime());
                session.setTicketTotal(sessionCmd.getTicketTotal());
                session.setTicketSurplus(sessionCmd.getTicketTotal());
                sessions.add(session);
            }
        }
        performance.setSessions(sessions);

        List<PerformanceStaff> staffList = new ArrayList<>();
        if (cmd.getStaffList() != null) {
            for (StaffCmd staffCmd : cmd.getStaffList()) {
                PerformanceStaff staff = new PerformanceStaff();
                staff.setPerformance(performance);
                staff.setStaffName(staffCmd.getStaffName());
                staff.setStaffType(staffCmd.getStaffType());
                staff.setStaffAvatar(staffCmd.getStaffAvatar());
                staff.setIntroduction(staffCmd.getIntroduction());
                staff.setSortOrder(staffCmd.getSortOrder());
                if (staffCmd.getUserId() != null) {
                    staff.setUser(userRepository.findById(staffCmd.getUserId()).orElse(null));
                }
                staffList.add(staff);
            }
        }
        performance.setStaffList(staffList);

        return performanceRepository.save(performance);
    }

    private void createApplicationRecord(UserInfo applicant, Performance performance, CreatePerformanceCmd cmd) {
        Application application = new Application();
        application.setApplicant(applicant);
        application.setApplicationType("PERFORMANCE_APPLY");
        application.setTargetId(performance.getId());
        application.setStatus(1);

        Map<String, Object> extra = new HashMap<>();
        extra.put("reason", cmd.getApplyReason());
        extra.put("performanceTitle", performance.getTitle());
        try {
            application.setExtraData(new ObjectMapper().writeValueAsString(extra));
        } catch (Exception e) {
            log.error("JSON序列化失败", e);
        }
        applicationRepository.save(application);
    }

    private void checkVenueConflict(Long venueId, LocalDateTime start, LocalDateTime end, Long excludeSessionId) {
        if (!start.isBefore(end)) throw new IllegalArgumentException("时间设置错误");
        List<PerformanceSession> conflicts = sessionRepository.findConflicts(venueId, start, end);
        for (PerformanceSession conflict : conflicts) {
            if (excludeSessionId != null && conflict.getId().equals(excludeSessionId)) continue;
            throw new IllegalStateException("排期冲突: " + conflict.getPerformance().getTitle());
        }
    }

    private void validateOrganizerAuthority(UserInfo user, String organizerType, Long organizerId) {
        if ("USER".equals(organizerType)) {
            if (!user.getId().equals(organizerId)) throw new SecurityException("权限不足");
        } else if ("ORGANIZATION".equals(organizerType)) {
            OrganizationInfo org = organizationInfoRepository.findById(organizerId)
                    .orElseThrow(() -> new IllegalArgumentException("社团不存在"));
            if (org.getLeader() == null || !org.getLeader().getId().equals(user.getId())) {
                throw new SecurityException("非社长无权申请");
            }
        }
    }
}