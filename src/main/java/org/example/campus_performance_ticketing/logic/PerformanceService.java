package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.*;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.CreatePerformanceCmd;
import org.example.campus_performance_ticketing.logic.dto.performance.SessionCmd;
import org.example.campus_performance_ticketing.logic.dto.performance.StaffCmd;
import org.example.campus_performance_ticketing.model.*;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Valid
public class PerformanceService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceSessionRepository sessionRepository;
    private final VenueRepository venueRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final VenueBlockedDayRepository venueBlockedDayRepository;

    private static final Logger logger = Logger.getLogger(PerformanceService.class.getName());


    @Value("${performance.post.temp-dir}")
    private String posterTempDir;

    @Value("${staff.photo.temp-dir}")
    private String staffTempDir;

    /**
     * 提交演出申请
     * @param userOpenId
     * @param cmd
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> submitPerformanceApplication(@NotBlank String userOpenId,
                                                          @Valid CreatePerformanceCmd cmd,
                                                          MultipartFile poster,
                                                          List<MultipartFile> staffPhotos) {
        try {
            UserInfo applicant = userRepository.findByOpenid(userOpenId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            if ("USER".equals(cmd.getOrganizerType())) {
                cmd.setOrganizerId(applicant.getId());
            }

            validateOrganizerAuthority(applicant, cmd.getOrganizerType(), cmd.getOrganizerId());

            // 检查演出场次是否冲突（时间范围内的闭馆日期和其他问题）
            validateSessionsAndBlockDays(cmd.getSessions());

            // === 核心修改：处理上传的文件 ===
            processUploadFiles(cmd, poster, staffPhotos);

            // 保存数据 (此时存的是 temp 路径)
            Performance performance = savePerformanceData(cmd, 0);

            createApplicationRecord(applicant, performance, cmd);

            ApiResponse<Void> response = ApiResponse.success(null);
            response.setMessage("演出申请提交成功，等待管理员审批");
            return response;
        } catch (Exception e) {
            logger.severe("演出申请提交失败: " + e.getMessage());
            return ApiResponse.fail("演出申请提交失败: " + e.getMessage());
        }
    }

    /**
     * 【供 AdminService 调用】直接创建演出并返回实体
     */
    @Transactional(rollbackFor = Exception.class)
    public Performance createPerformanceEntity(CreatePerformanceCmd cmd) {
        // 复用已有的保存逻辑，直接状态设为 1 (已发布)
        return savePerformanceData(cmd, 1);
    }

    /**
     * 保存演出数据
     * @param cmd
     * @param status
     * @return
     */
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
                Venue venue = venueRepository.findById(sessionCmd.getVenueId()).orElseThrow();
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
        } catch (Exception ignored) {}
        applicationRepository.save(application);
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


    /**
     * 验证场次是否冲突，包括与场馆闭馆日期冲突.
     *
     * @param sessions 场次列表
     */
    private void validateSessionsAndBlockDays(List<SessionCmd> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            throw new IllegalArgumentException("演出场次不得为空");
        }

        for (SessionCmd session : sessions) {
            // 检查场馆是否存在
            Venue venue = venueRepository.findById(session.getVenueId())
                    .orElseThrow(() -> new IllegalArgumentException("指定的场馆不存在"));

            // 验证场馆是否在演出时间段内被屏蔽
            LocalDateTime startTime = session.getStartTime();
            LocalDateTime endTime = session.getEndTime();
            validateIfBlocked(venue, startTime, endTime);

            // 检查是否与其他场次冲突
            checkVenueConflict(session.getVenueId(), session.getStartTime(), session.getEndTime(), null);
        }
    }

    /**
     * 验证演出是否与场馆的屏蔽日期冲突
     *
     * @param venue 场馆
     * @param startTime 场次开始时间
     * @param endTime 场次结束时间
     */
    private void validateIfBlocked(Venue venue, LocalDateTime startTime, LocalDateTime endTime) {
        // 获取场次时间段内是否存在与屏蔽日期重叠
        List<VenueBlockedDay> blockedDays = venueBlockedDayRepository.findByVenueIdAndBlockedDateBetween(
                venue.getId(),
                startTime.toLocalDate(),
                endTime.toLocalDate()
        );

        if (!blockedDays.isEmpty()) {
            for (VenueBlockedDay blockedDay : blockedDays) {
                LocalDate blockedDate = blockedDay.getBlockedDate();
                if (!endTime.toLocalDate().isBefore(blockedDate) && !startTime.toLocalDate().isAfter(blockedDate)) {
                    throw new IllegalArgumentException(
                            "场馆 [" + venue.getName() + "] 在 " + blockedDate + " 已闭馆，无法申请演出"
                    );
                }
            }
        }
    }

    private void checkVenueConflict(Long venueId, LocalDateTime start, LocalDateTime end, Long excludeSessionId) {
        if (!start.isBefore(end)) throw new IllegalArgumentException("时间设置错误");
        List<PerformanceSession> conflicts = sessionRepository.findConflicts(venueId, start, end);
        for (PerformanceSession conflict : conflicts) {
            if (excludeSessionId != null && conflict.getId().equals(excludeSessionId)) continue;
            throw new IllegalStateException("排期冲突: " + conflict.getPerformance().getTitle());
        }
    }

    /**
     * 私有方法：集中处理文件上传
     * 利用 FileUtil.saveImage 将文件保存到临时目录，并回填路径到 cmd
     */
    private void processUploadFiles(CreatePerformanceCmd cmd, MultipartFile poster, List<MultipartFile> staffPhotos) {
        // 1. 处理海报
        if (poster != null && !poster.isEmpty()) {
            try {
                // 直接使用 FileUtil 保存到临时目录
                String posterPath = FileUtil.saveImage(poster, posterTempDir);
                cmd.setPosterUrl(posterPath);
            } catch (IOException e) {
                logger.severe("海报保存失败: " + e.getMessage());
                throw new RuntimeException("海报上传失败");
            }
        }

        // 2. 处理演职人员照片
        // 逻辑：前端在 JSON 的 staffAvatar 字段填的是文件名，后端根据文件名去 staffPhotos 列表里找文件
        if (staffPhotos != null && !staffPhotos.isEmpty() && cmd.getStaffList() != null) {
            // 转为 Map 方便查找
            Map<String, MultipartFile> fileMap = staffPhotos.stream()
                    .collect(Collectors.toMap(MultipartFile::getOriginalFilename, f -> f, (k1, k2) -> k1));

            for (StaffCmd staff : cmd.getStaffList()) {
                String originalFilename = staff.getStaffAvatar(); // 前端填的文件名
                if (StringUtils.hasText(originalFilename) && fileMap.containsKey(originalFilename)) {
                    try {
                        String path = FileUtil.saveImage(fileMap.get(originalFilename), staffTempDir);
                        staff.setStaffAvatar(path); // 替换为真实路径
                    } catch (IOException e) {
                        logger.severe("人员照片保存失败: " + originalFilename);
                        throw new RuntimeException("演职人员照片上传失败: " + originalFilename);
                    }
                }
            }
        }
    }


}