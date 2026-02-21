package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.*;
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
import java.util.*;
import java.util.logging.Logger;

@Slf4j
@Service
@RequiredArgsConstructor
@Valid
public class PerformanceApplyService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceSessionRepository sessionRepository;
    private final VenueRepository venueRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final VenueBlockedDayRepository venueBlockedDayRepository;

    private static final Logger logger = Logger.getLogger(PerformanceApplyService.class.getName());

    @Value("${performance.post.temp-dir}")
    private String posterTempDir;

    @Value("${staff.photo.temp-dir}")
    private String staffTempDir;

    /**
     * 第一步：只提交 CreatePerformanceCmd，先创建演出“草稿/待审批记录”，返回 performanceId
     *
     * 注意：
     * - 因为海报改为单独上传，所以这里不再依赖 cmd.posterUrl
     * - 你可以选择：
     *   A) performance.publishStatus=0 先落库，posterUrl 为空，等 uploadPoster 再补
     *   B) 或者创建 Application 但不创建 Performance（不推荐，会改动更大）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createApplicationDraftReturnId(@NotBlank String userOpenId,
                                               @Valid CreatePerformanceCmd cmd) {

        UserInfo applicant = userRepository.findByOpenid(userOpenId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if ("USER".equals(cmd.getOrganizerType())) {
            if (applicant.getStatus() != 1) {
                throw new SecurityException("用户状态异常，无法申请演出");
            }
            cmd.setOrganizerId(applicant.getId());
        }

        validateOrganizerAuthority(applicant, cmd.getOrganizerType(), cmd.getOrganizerId());

        validateSessionsAndBlockDays(cmd.getSessions());

        // posterUrl 先置空，等 uploadPoster 再补
        cmd.setPosterUrl(null);

        Performance performance = savePerformanceData(cmd, 0);

        createApplicationRecord(applicant, performance, cmd);

        return performance.getId();
    }

    /**
     * 第三步：逐张上传演职人员照片
     *
     * 简单策略：按顺序填充 staffList 中第一个 staffAvatar 为空的 staff
     * 若都不为空，则追加一个 staff 记录避免丢图
     */
    @Transactional(rollbackFor = Exception.class)
    public void appendStaffPhoto(@NotBlank String userOpenId,
                                 Long performanceId,
                                 MultipartFile staffPhoto) {

        if (staffPhoto == null || staffPhoto.isEmpty()) {
            throw new IllegalArgumentException("staffPhotos 文件不能为空");
        }

        UserInfo applicant = userRepository.findByOpenid(userOpenId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Performance performance = performanceRepository.findById(performanceId)
                .orElseThrow(() -> new IllegalArgumentException("演出不存在"));

        validateOrganizerAuthority(applicant, performance.getOrganizerType(), performance.getOrganizerId());

        String path;
        try {
            path = FileUtil.saveImage(staffPhoto, staffTempDir);
        } catch (IOException e) {
            throw new RuntimeException("演职人员照片上传失败: " + e.getMessage(), e);
        }

        List<PerformanceStaff> staffList = performance.getStaffList();
        if (staffList == null) staffList = new ArrayList<>();

        PerformanceStaff target = null;
        for (PerformanceStaff s : staffList) {
            if (!StringUtils.hasText(s.getStaffAvatar())) {
                target = s;
                break;
            }
        }

        if (target != null) {
            target.setStaffAvatar(path);
        } else {
            PerformanceStaff extra = new PerformanceStaff();
            extra.setPerformance(performance);
            extra.setStaffName("演职人员");
            extra.setStaffType("STAFF_PHOTO");
            extra.setStaffAvatar(path);
            extra.setIntroduction("");
            extra.setSortOrder(staffList.size() + 1);
            staffList.add(extra);
        }

        performance.setStaffList(staffList);
        performanceRepository.save(performance);
    }

    /**
     * 【供 AdminPerformanceService 调用】直接创建演出并返回实体（不走“申请单”流程）
     *
     * 适用场景：
     * - 管理员强制征用/直接发布演出
     *
     * 行为：
     * - 复用 savePerformanceData
     * - 默认 publishStatus=1（已发布）。如果你们后台约定别的状态，请改这个值。
     * - 不做申请人/负责人权限校验（因为管理员入口已校验）
     * - 仍然做场次冲突/闭馆校验（避免创建非法排期）。如果你希望管理员可以无视闭馆/冲突，可删掉校验。
     *
     * 注意：
     * - 这里不会处理文件上传（poster/staff 照片），因为管理员创建通常走 URL 或后续上传流程。
     *   如果 cmd.posterUrl 本身带了 URL/路径，会原样入库。
     */
    @Transactional(rollbackFor = Exception.class)
    public Performance createPerformanceEntity(@Valid CreatePerformanceCmd cmd) {
        // 1) 校验场次（复用你已有逻辑）
        validateSessionsAndBlockDays(cmd.getSessions());

        // 2) 直接保存并发布
        // 1 = 已发布（按你 controller 的注释“默认建议传 1-已发布”）
        return savePerformanceData(cmd, 1);
    }

    // ====== 你原有逻辑（保存/校验/审批单）保持一致 ======

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
                session.setStatus(0);
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
        application.setStatus(1); // 1-待审核

        // extraData：用键值对(JSON)存储额外信息，方便后台审核页面展示/追溯
        Map<String, Object> extra = new HashMap<>();

        // 申请理由（按键值对写入）
        extra.put("applyReason", cmd.getApplyReason() == null ? "" : cmd.getApplyReason());

        // 你原来已有的字段也可以保留（可选）
        extra.put("performanceTitle", performance.getTitle());
        extra.put("description", performance.getDescription());

        // 场次与场地信息（可选，保留你原逻辑）
        if (performance.getSessions() != null && !performance.getSessions().isEmpty()) {
            List<Map<String, Object>> sessionsInfo = new ArrayList<>();
            for (PerformanceSession session : performance.getSessions()) {
                Map<String, Object> sessionMap = new HashMap<>();
                sessionMap.put("startTime", session.getStartTime() == null ? null : session.getStartTime().toString());
                sessionMap.put("endTime", session.getEndTime() == null ? null : session.getEndTime().toString());
                sessionMap.put("venueId", session.getVenue() == null ? null : session.getVenue().getId());
                sessionMap.put("venueName", session.getVenue() == null ? null : session.getVenue().getName());
                sessionsInfo.add(sessionMap);
            }
            extra.put("sessions", sessionsInfo);

            if (performance.getSessions().get(0).getVenue() != null) {
                extra.put("primaryVenueName", performance.getSessions().get(0).getVenue().getName());
            }
        }

        try {
            application.setExtraData(new ObjectMapper().writeValueAsString(extra));
        } catch (Exception e) {
            logger.warning("申请单 extraData 序列化失败: " + e.getMessage());
            // 这里不建议直接吞掉导致 extraData 为空，你也可以选择抛异常回滚
        }

        applicationRepository.save(application);
    }

    private void validateOrganizerAuthority(UserInfo user, String organizerType, Long organizerId) {
        if ("USER".equals(organizerType)) {
            if (!user.getId().equals(organizerId)) throw new SecurityException("权限不足");
        } else if ("ORGANIZATION".equals(organizerType)) {
            OrganizationInfo org = organizationInfoRepository.findById(organizerId)
                    .orElseThrow(() -> new IllegalArgumentException("社团不存在"));

            if (org.getStatus() != 1) {
                throw new SecurityException("社团状态异常，无法申请演出");
            }

            if (org.getLeader() == null || !org.getLeader().getId().equals(user.getId())) {
                throw new SecurityException("非社长无权申请");
            }
        } else {
            throw new IllegalArgumentException("organizerType 不合法: " + organizerType);
        }
    }

    private void validateSessionsAndBlockDays(List<SessionCmd> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            throw new IllegalArgumentException("演出场次不得为空");
        }

        for (SessionCmd session : sessions) {
            Venue venue = venueRepository.findById(session.getVenueId())
                    .orElseThrow(() -> new IllegalArgumentException("指定的场馆不存在"));

            LocalDateTime startTime = session.getStartTime();
            LocalDateTime endTime = session.getEndTime();

            validateIfBlocked(venue, startTime, endTime);

            checkVenueConflict(session.getVenueId(), startTime, endTime, null);
        }
    }

    private void validateIfBlocked(Venue venue, LocalDateTime startTime, LocalDateTime endTime) {
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
}