package org.example.campus_performance_ticketing.logic;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.PerformanceSessionRepository;
import org.example.campus_performance_ticketing.dao.TicketTemplateRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.ticket.TicketTemplateUpdateDTO;
import org.example.campus_performance_ticketing.logic.dto.ticket.TicketTemplateUploadDTO;
import org.example.campus_performance_ticketing.model.*;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketTemplateService {

    private final TicketTemplateRepository ticketTemplateRepository;
    private final PerformanceSessionRepository performanceSessionRepository;
    private final UserRepository userInfoRepository;
    private final OrganizationInfoRepository organizationInfoRepository;

    @Value("${ticket.photo.upload-dir}")
    private String uploadDir;

    @Value("${file.base.url}")
    private String fileBaseUrl;

    /**
     * 创建或更新电子票模板
     * 权限控制：仅管理员、超级管理员、或该场演出的组织者（个人或组织负责人）可上传
     *
     * @param openId 操作用户的 OpenID
     * @param dto    上传数据传输对象
     * @return ApiResponse
     */
    @Transactional(rollbackOn = Exception.class)
    public ApiResponse<Void> createOrUpdateTicketTemplate(@NotBlank String openId,
                                                          @Valid TicketTemplateUploadDTO dto,
                                                          MultipartFile imageFile) {
        // 1. 基础校验
        if (imageFile == null || imageFile.isEmpty()) {
            return ApiResponse.fail("上传的图片文件不能为空");
        }

        // 2. 获取操作用户信息及权限校验
        UserInfo user = userInfoRepository.findByOpenid(openId)
                .orElse(null);
        if (user == null) {
            return ApiResponse.fail("操作用户不存在");
        }

        // 判断是否为全局管理员
        boolean isGlobalAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(user.getRole());

        // 3. 查询涉及的场次
        List<PerformanceSession> sessions = performanceSessionRepository.findAllById(dto.getSessionIds());
        if (sessions.isEmpty()) {
            return ApiResponse.fail("未找到有效的演出场次");
        }
        if (sessions.size() != dto.getSessionIds().size()) {
            return ApiResponse.fail("部分场次ID无效，请刷新后重试");
        }

        // 4. 针对非管理员，逐个检查是否为对应演出的组织者
        if (!isGlobalAdmin) {
            for (PerformanceSession session : sessions) {
                Performance performance = session.getPerformance();
                if (performance == null) {
                    return ApiResponse.fail("场次数据异常：未关联演出信息");
                }

                // 检查组织者权限
                boolean isOrganizer = checkIsOrganizer(user, performance);
                if (!isOrganizer) {
                    return ApiResponse.fail("无权操作：您不是演出 [" + performance.getTitle() + "] 的组织者 或 个人/组织状态异常");
                }
            }
        }

        // 5. 保存物理文件 (只保存一次，供多个场次复用)
        String savedPath;
        try {
            // 使用 FileUtil 保存图片
            savedPath = FileUtil.saveImage(imageFile, uploadDir);
            if (savedPath == null) {
                return ApiResponse.fail("文件保存失败，路径为空");
            }
        } catch (IOException e) {
            log.error("电子票背景图保存失败", e);
            return ApiResponse.fail("文件上传发生错误: " + e.getMessage());
        }

        // 6. 为每个场次创建或更新数据库记录
        for (PerformanceSession session : sessions) {
            saveTemplateForSession(session, savedPath, dto.getStatus());
        }

        ApiResponse<Void> response = ApiResponse.success(null);
        response.setMessage("电子票模板上传成功");

        return response;
    }

    /**
     * 更新电子票模板（支持 修改图片 和 修改状态）
     * 场景：更换背景图、上下架操作
     */
    @Transactional(rollbackOn = Exception.class)
    public ApiResponse<Void> updateTicketTemplate(@NotBlank String openId,
                                                  @Valid TicketTemplateUpdateDTO dto,
                                                  MultipartFile imageFile) {
        // 1. 基础参数校验
        if (dto.getSessionIds() == null || dto.getSessionIds().isEmpty()) {
            return ApiResponse.fail("场次ID不能为空");
        }

        // 2. 获取操作用户信息
        UserInfo user = userInfoRepository.findByOpenid(openId).orElse(null);
        if (user == null) {
            return ApiResponse.fail("操作用户不存在");
        }
        boolean isGlobalAdmin = "ADMIN".equalsIgnoreCase(user.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(user.getRole());

        // 3. 查询涉及的场次
        List<PerformanceSession> sessions = performanceSessionRepository.findAllById(dto.getSessionIds());
        if (sessions.isEmpty()) {
            return ApiResponse.fail("未找到有效的演出场次");
        }

        // 4. 权限校验
        if (!isGlobalAdmin) {
            for (PerformanceSession session : sessions) {
                Performance performance = session.getPerformance();
                if (performance == null) return ApiResponse.fail("数据异常：场次未关联演出");

                if (!checkIsOrganizer(user, performance)) {
                    return ApiResponse.fail("无权操作：您不是演出 [" + performance.getTitle() + "] 的组织者");
                }
            }
        }

        // 5. 处理新图片保存 (如果有)
        String newImagePath = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                newImagePath = FileUtil.saveImage(imageFile, uploadDir);
            } catch (IOException e) {
                log.error("更新图片失败", e);
                return ApiResponse.fail("图片保存失败: " + e.getMessage());
            }
        }

        // 6. 执行更新 (复用逻辑)
        int updatedCount = 0;
        for (PerformanceSession session : sessions) {
            boolean success = saveTemplateForSession(session, newImagePath, dto.getStatus());
            if (success) updatedCount++;
        }

        if (updatedCount == 0 && sessions.size() > 0) {
            return ApiResponse.fail("更新失败：未找到原模板，且未提供新图片来创建模板");
        }

        ApiResponse<Void> response = ApiResponse.success(null);
        response.setMessage("电子票模板更新成功");

        return response;
    }

    /**
     * 获取指定场次当前生效（已上架）的电子票背景图 URL
     * 场景：用户查看票面、前端渲染电子票
     *
     * @param sessionId 场次ID
     * @return 成功返回 URL，如果未设置或已下架则返回 null data
     */
    public ApiResponse<String> getActiveTicketTemplateUrl(Long sessionId) {
        if (sessionId == null) {
            return ApiResponse.fail("场次 ID 不能为空");
        }

        // 1. 直接查询指定场次且状态为 1 (上架) 的模板
        // 假设 Repository 中已有 findBySessionIdAndStatus 方法
        // 如果没有，请在 TicketTemplateRepository 中添加: Optional<TicketTemplate> findBySessionIdAndStatus(Long sessionId, Integer status);
        Optional<TicketTemplate> templateOp = ticketTemplateRepository.findBySessionIdAndStatus(sessionId, 1);

        if (templateOp.isEmpty()) {
            // 未找到或已下架 -> 返回空数据
            ApiResponse<String> response = ApiResponse.success(null);
            response.setMessage("该场次暂无生效的电子票模板");
            return response;
        }

        // 2. 仅返回 URL
        String url = AvatarUrlUtil.buildAvatarUrl(templateOp.get().getBackgroundImgUrl(), fileBaseUrl);
        return ApiResponse.success(url);
    }


    /**
     * 检查用户是否为演出的组织者
     */
    private boolean checkIsOrganizer(UserInfo user, Performance performance) {
        String type = performance.getOrganizerType();
        Long organizerId = performance.getOrganizerId();

        // 场景 1: 个人举办
        if ("USER".equalsIgnoreCase(type)) {

            if (user.getStatus() != 1) {
                log.warn("演出关联的个人用户ID [{}] 状态异常", organizerId);
                return false;
            }

            return organizerId.equals(user.getId());
        }

        // 场景 2: 组织举办
        if ("ORGANIZATION".equalsIgnoreCase(type)) {
            // 查询组织信息
            Optional<OrganizationInfo> orgOpt = organizationInfoRepository.findById(organizerId);
            if (orgOpt.isEmpty()) {
                log.warn("演出关联的组织ID [{}] 不存在", organizerId);
                return false;
            }
            OrganizationInfo org = orgOpt.get();

            // 检查组织状态是否正常（可选，根据业务需求）
            if (org.getStatus() != 1) {
                log.warn("演出关联的组织ID [{}] 状态异常", organizerId);
                return false;
            }

            // 检查当前用户是否为该组织的 Leader
            // 注意：org.getLeader() 也是 UserInfo 对象，需比较 ID
            return org.getLeader().getId().equals(user.getId());
        }

        return false;
    }

    /**
     * 通用保存/更新方法
     * @param imagePath 新图片路径 (传 null 表示不修改图片)
     * @param status 新状态 (传 null 表示不修改状态)
     * @return 是否成功更新/创建
     */
    private boolean saveTemplateForSession(PerformanceSession session, String imagePath, Integer status) {
        Optional<TicketTemplate> existingOp = ticketTemplateRepository.findBySessionId(session.getId());
        TicketTemplate template;

        if (existingOp.isPresent()) {
            // === 场景 A: 更新现有模板 ===
            template = existingOp.get();

            // 1. 如果有新图，且路径不同 -> 删旧图，换新图
            if (imagePath != null && !imagePath.equals(template.getBackgroundImgUrl())) {
                FileUtil.deletePhysicalFile(template.getBackgroundImgUrl());
                template.setBackgroundImgUrl(imagePath);
            }

            // 2. 如果有新状态 -> 更新状态
            if (status != null) {
                template.setStatus(status);
            }
        } else {
            // === 场景 B: 尚未有模板 (新建) ===
            // 必须有图片才能新建。如果只传了状态但没图，无法创建记录。
            if (imagePath == null) {
                log.warn("场次 [{}] 无模板记录且未上传图片，无法执行更新", session.getId());
                return false;
            }

            template = new TicketTemplate();
            template.setSession(session);
            template.setBackgroundImgUrl(imagePath);
            template.setStatus(status != null ? status : 1); // 新建默认上架
        }

        ticketTemplateRepository.save(template);
        log.info("场次 [{}] 电子票模板已保存", session.getId());
        return true;
    }
}