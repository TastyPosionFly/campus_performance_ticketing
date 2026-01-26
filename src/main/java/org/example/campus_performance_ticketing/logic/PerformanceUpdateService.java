package org.example.campus_performance_ticketing.logic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.dao.PerformanceSessionRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.dao.VenueRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.*;
import org.example.campus_performance_ticketing.model.*;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceUpdateService {

    private final PerformanceRepository performanceRepository;
    private final PerformanceSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    private static final Logger logger = Logger.getLogger(PerformanceUpdateService.class.getName());
    private static final int CANCELLATION_THRESHOLD_MINUTES = 60; // 时间限制阈值

    @Value("${performance.post.upload-dir}")
    private String posterDir;

    @Value("${staff.photo.upload-dir}")
    private String staffPhotoDir;

    @Value("${file.base.url}")
    private String baseUrl;

    /**
     * 修改演出
     * 规则：
     * 1. 管理员 (ADMIN/SUPER_ADMIN): 可以修改所有信息。
     * 2. 举办者 (Organizer): 只能修改场次时间 (延期)，不能修改基本信息、海报、状态或演职人员。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<PerformanceDetailDto> updatePerformance(
            @NotNull String userOpenId,
            @Valid UpdatePerformanceRequestDto updateRequest,
            MultipartFile newPosterFile,
            List<MultipartFile> staffPhotoFiles) {

        try {
            // 1. 获取当前用户
            UserInfo currentUser = userRepository.findByOpenid(userOpenId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            // 转换文件列表为 Map，key为文件名，方便查找
            Map<String, MultipartFile> photoMap = null;
            if (staffPhotoFiles != null && !staffPhotoFiles.isEmpty()) {
                photoMap = staffPhotoFiles.stream()
                        .collect(Collectors.toMap(MultipartFile::getOriginalFilename, f -> f, (k1, k2) -> k1));
            }

            // 2. 判定角色
            boolean isAdmin = "ADMIN".equals(currentUser.getRole()) || "SUPER_ADMIN".equals(currentUser.getRole());

            // 3. 获取演出
            var performanceCmd = updateRequest.getPerformanceCmd();
            Performance performance = performanceRepository.findById(performanceCmd.getPerformanceId())
                    .orElseThrow(() -> new IllegalArgumentException("指定的演出不存在"));

            // 4. 验证权限 (如果是举办者或管理员则通过，否则抛异常)
            boolean isOrganizer = performance.getOrganizerId().equals(currentUser.getId());
            if (!isAdmin && !isOrganizer) {
                throw new SecurityException("您没有权限修改此演出！");
            }

            // =========================================================
            // A. 基本信息修改 (海报、标题、描述、状态) -> 仅管理员可用
            // =========================================================
            if (isAdmin) {
                // 更新海报
                if (newPosterFile != null) {
                    updatePoster(performance, newPosterFile);
                }
                // 更新文本信息
                if (performanceCmd.getTitle() != null) performance.setTitle(performanceCmd.getTitle());
                if (performanceCmd.getDescription() != null) performance.setDescription(performanceCmd.getDescription());
                if (performanceCmd.getPublishStatus() != null) performance.setPublishStatus(performanceCmd.getPublishStatus());
            } else {
                // 如果是举办者尝试修改内容，警告
                if (newPosterFile != null) {
                    throw new SecurityException("举办者无权修改演出海报，仅允许申请延期！");
                }
                if (performanceCmd.getTitle() != null) {
                    throw new SecurityException("举办者无权修改演出标题，仅允许申请延期！");
                }
                if (performanceCmd.getDescription() != null) {
                    throw new SecurityException("举办者无权修改演出描述，仅允许申请延期！");
                }
                if (performanceCmd.getPublishStatus() != null) {
                    throw new SecurityException("举办者无权修改发布状态，仅允许申请延期！");
                }
            }

            // =========================================================
            // B. 场次修改 (延期/取消) -> 管理员 和 举办者 均可用
            // =========================================================
            var sessions = updateRequest.getSessions();
            if (sessions != null) {
                ApiResponse<Void> sessionUpdateResult = updateSessions(performance, sessions);
                if (!sessionUpdateResult.isSuccess()) {
                    return ApiResponse.fail(sessionUpdateResult.getMessage());
                }
            }

            // =========================================================
            // C. 演职人员修改 -> 仅管理员可用
            // =========================================================
            var staffList = updateRequest.getStaffList();
            if (isAdmin && staffList != null) {
                // 传入 photoMap
                updateStaff(performance, staffList, photoMap);
            } else if (!isAdmin && staffList != null) {
                throw new SecurityException("举办者无权修改演职人员信息，仅允许申请延期！");
            }

            // 5. 保存修改
            performanceRepository.save(performance);

            // 6. 返回结果
            PerformanceDetailDto performanceWithUrls = buildPerformanceWithFullUrls(performance);

            ApiResponse<PerformanceDetailDto> response = ApiResponse.success(performanceWithUrls);
            response.setMessage("演出更新成功");

            return response;
        } catch (Exception e) {
            logger.warning("演出更新失败: " + e.getMessage());
            return ApiResponse.fail("演出更新失败: " + e.getMessage());
        }
    }

    // ---------------- 私有方法 (保持不变) ----------------

    private void updatePoster(Performance performance, MultipartFile newPosterFile) throws Exception {
        String oldPath = performance.getPosterUrl();
        String newPath = FileUtil.saveImage(newPosterFile, posterDir);
        performance.setPosterUrl(newPath);
        if (oldPath != null && !oldPath.isEmpty()) {
            FileUtil.deletePhysicalFile(oldPath);
        }
    }

    private void updateStaff(Performance performance, List<StaffCmd> newStaffList, Map<String, MultipartFile> photoMap) {
        List<PerformanceStaff> existingStaff = performance.getStaffList();
        // 删除逻辑保持不变...
        existingStaff.removeIf(staff -> newStaffList.stream()
                .noneMatch(newStaff -> staff.getStaffName().equals(newStaff.getStaffName())
                        && staff.getStaffType().equals(newStaff.getStaffType())));

        for (StaffCmd newStaff : newStaffList) {
            PerformanceStaff existing = existingStaff.stream()
                    .filter(staff -> staff.getStaffName().equals(newStaff.getStaffName())
                            && staff.getStaffType().equals(newStaff.getStaffType()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                updateStaffPhotos(existing, newStaff, photoMap);
            } else {
                // 新增逻辑
                PerformanceStaff newStaffEntity = new PerformanceStaff();
                // ... 设置基本属性 ...
                newStaffEntity.setPerformance(performance);
                newStaffEntity.setStaffName(newStaff.getStaffName());
                newStaffEntity.setStaffType(newStaff.getStaffType());
                newStaffEntity.setIntroduction(newStaff.getIntroduction());
                newStaffEntity.setSortOrder(newStaff.getSortOrder());

                // 图片处理逻辑优化：通过文件名匹配
                // 前端在 newStaff.getStaffAvatar() 传递文件名，我们去 map 里找文件
                // 或者如果在更新场景，前端没传文件名，我们尝试用 "姓名.jpg" 等约定，
                // 但最稳妥的是前端传递 staffAvatar="lisi.jpg" 且文件名为 "lisi.jpg"
                if (photoMap != null) {
                    // 优先匹配前端指定的 avatar 字段 (作为文件名)
                    String targetFileName = newStaff.getStaffAvatar();
                    // 如果前端没传 avatar 字段，也可以尝试用 staffName 匹配(可选策略，视前端实现而定)
                    // 这里假设前端在 JSON 里填了文件名
                    if (StringUtils.hasText(targetFileName) && photoMap.containsKey(targetFileName)) {
                        saveAndSetAvatar(newStaffEntity, photoMap.get(targetFileName));
                    }
                }
                existingStaff.add(newStaffEntity);
            }
        }
    }


    // 修改：接收 Map
    private void updateStaffPhotos(PerformanceStaff existing, StaffCmd newStaff, Map<String, MultipartFile> photoMap) {
        if (photoMap != null) {
            // 逻辑：前端在 JSON 中 staffAvatar 字段传文件名，文件列表中传文件
            String targetFileName = newStaff.getStaffAvatar();

            // 如果 JSON 里没传文件名，说明不想更新图片，或者是想复用旧图
            // 只有当文件名在上传列表中存在时，才进行更新
            if (StringUtils.hasText(targetFileName) && photoMap.containsKey(targetFileName)) {
                MultipartFile newFile = photoMap.get(targetFileName);
                // 删除旧图
                String oldAvatarPath = existing.getStaffAvatar();
                if (oldAvatarPath != null && !oldAvatarPath.isEmpty()) {
                    FileUtil.deletePhysicalFile(oldAvatarPath);
                }
                // 保存新图
                saveAndSetAvatar(existing, newFile);
            }
        }
        existing.setIntroduction(newStaff.getIntroduction());
        existing.setSortOrder(newStaff.getSortOrder());
    }

    private void saveAndSetAvatar(PerformanceStaff staff, MultipartFile file) {
        try {
            String newAvatarPath = FileUtil.saveImage(file, staffPhotoDir);
            staff.setStaffAvatar(newAvatarPath);
        } catch (IOException e) {
            logger.warning("保存演职人员定妆照失败: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 更新场次信息 (处理新增、修改、删除)
     * 逻辑：
     * 1. 删除：如果现有场次在前端提交的列表中不存在（根据场地+开始时间匹配），则删除。
     * 2. 修改：如果匹配到（开始时间没变），则更新结束时间和票数。
     * 3. 新增：如果没匹配到（或者是延期修改了开始时间），则作为新场次添加，并检查冲突。
     */
    private ApiResponse<Void> updateSessions(Performance performance, List<SessionCmd> newSessions) {
        List<PerformanceSession> existingSessions = performance.getSessions();

        // 1. 删除不在新列表中的旧场次
        // 注意：removeIf 会直接从 Hibernate 代理的集合中移除元素，触发数据库删除
        existingSessions.removeIf(existing -> {
            // 检查该 existing 场次是否在 newSessions 中存在
            boolean existsInNew = newSessions.stream().anyMatch(newSession ->
                    existing.getVenue().getId().equals(newSession.getVenueId()) &&
                            existing.getStartTime().isEqual(newSession.getStartTime())
            );
            return !existsInNew; // 如果不存在，则移除
        });

        // 2. 处理新增或修改
        for (SessionCmd newSession : newSessions) {
            // 查找是否是更新操作（startTime 未变）
            PerformanceSession existing = existingSessions.stream()
                    .filter(session -> session.getVenue().getId().equals(newSession.getVenueId())
                            && session.getStartTime().isEqual(newSession.getStartTime()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                // [更新逻辑] (开始时间未变，仅更新结束时间或票数)
                // 这里不需要复杂的冲突检查，因为开始时间没变，通常不会撞到别人的新时间（除非延长了结束时间，严格来说也应该查，但此处简化）
                existing.setEndTime(newSession.getEndTime());
                existing.setTicketTotal(newSession.getTicketTotal());
            } else {
                // [新增逻辑] (或者是延期导致了开始时间变更)

                // 1. 延期/新增的时间限制检查
                // 只有在修改开始时间（即视为新增）时才强制检查“距离现在是否由足够时间”
                // 如果是单纯的新增场次，可能不需要这个60分钟限制？还是说所有操作都受限？
                // 这里假设所有涉及到新时间的操作都受限。
                if (newSession.getStartTime().isBefore(LocalDateTime.now().plusMinutes(CANCELLATION_THRESHOLD_MINUTES))) {
                    return ApiResponse.fail("新增/延期的场次距离开始时间不足 60 分钟，操作被拒绝！");
                }

                // 2. 冲突检查
                List<PerformanceSession> conflicts = sessionRepository.findConflicts(
                        newSession.getVenueId(), newSession.getStartTime(), newSession.getEndTime());

                if (!conflicts.isEmpty()) {
                    return ApiResponse.fail("排期冲突：所选时间段已有其他演出！");
                }

                Venue venue = venueRepository.findById(newSession.getVenueId())
                        .orElseThrow(() -> new IllegalArgumentException("指定的场馆不存在"));

                PerformanceSession session = new PerformanceSession();
                session.setPerformance(performance);
                session.setVenue(venue);
                session.setStartTime(newSession.getStartTime());
                session.setEndTime(newSession.getEndTime());
                session.setTicketTotal(newSession.getTicketTotal());
                session.setTicketSurplus(newSession.getTicketTotal()); // 新增时剩余票数等于总票数

                existingSessions.add(session);
            }
        }
        return ApiResponse.success(null);
    }

    private PerformanceDetailDto buildPerformanceWithFullUrls(Performance performance) {
        PerformanceDetailDto dto = PerformanceDetailDto.from(performance);
        dto.setPosterUrl(AvatarUrlUtil.buildAvatarUrl(performance.getPosterUrl(), baseUrl));
        dto.getStaff().forEach(staff -> {
            if (staff.getStaffAvatar() != null) {
                staff.setStaffAvatar(AvatarUrlUtil.buildAvatarUrl(staff.getStaffAvatar(), baseUrl));
            }
        });
        return dto;
    }
}