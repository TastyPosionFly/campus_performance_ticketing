package org.example.campus_performance_ticketing.logic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.PerformanceMediaLinkRepository;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance_media_link.CreateMediaLinkCmd;
import org.example.campus_performance_ticketing.logic.dto.performance_media_link.MediaLinkDto;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceMediaLink;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Valid
@RequiredArgsConstructor
public class PerformanceMediaLinkService {

    private final PerformanceMediaLinkRepository mediaLinkRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    /**
     * 添加/上传 演出媒体外链
     * 策略：强制单平台单类型唯一。
     * 如果 [Type + Platform] 组合已存在，则视为更新原记录。
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> addMediaLink(@NotBlank String operatorOpenId,
                                          @Valid CreateMediaLinkCmd cmd) {
        try {
            // 1. 获取用户信息
            UserInfo operator = userRepository.findByOpenid(operatorOpenId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            // 2. 获取演出信息
            Performance performance = performanceRepository.findById(cmd.getPerformanceId())
                    .orElseThrow(() -> new IllegalArgumentException("演出不存在"));

            // 3. 权限校验
            if (!hasPermission(operator, performance)) {
                throw new SecurityException("无权操作：您不是该演出的举办者或管理员");
            }

            // 4. 【核心逻辑】唯一性校验与更新策略
            // 规则：同一个演出下，[类型+平台] 必须唯一。
            // 查找该演出下的所有链接
            List<PerformanceMediaLink> existingLinks = mediaLinkRepository
                    .findByPerformanceIdOrderBySortOrderDesc(performance.getId());

            // 在内存中过滤：查找是否有同类型、同平台的记录
            PerformanceMediaLink mediaLink = existingLinks.stream()
                    .filter(link -> link.getType().equals(cmd.getType()) && link.getPlatform().equals(cmd.getPlatform()))
                    .findFirst()
                    .orElse(new PerformanceMediaLink());

            if (mediaLink.getId() == null) {
                // 如果不存在，则是新增
                mediaLink.setPerformance(performance);
                mediaLink.setType(cmd.getType());
                mediaLink.setPlatform(cmd.getPlatform());
            } else {
                // 如果存在，则是更新
                log.info("检测到已存在 [类型:{}][平台:{}] 的外链，执行覆盖更新: id={}, oldKey={}, newKey={}",
                        cmd.getType(), cmd.getPlatform(), mediaLink.getId(), mediaLink.getExternalKey(), cmd.getExternalKey());
            }

            // 5. 更新具体内容 (无论是新增还是更新，都刷新以下字段)
            mediaLink.setExternalKey(cmd.getExternalKey()); // 更新 URL
            mediaLink.setTitle(cmd.getTitle());             // 更新 标题
            mediaLink.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
            mediaLink.setAppId(cmd.getAppId());
            mediaLink.setPath(cmd.getPath());

            mediaLinkRepository.save(mediaLink);
            return ApiResponse.success(null);

        } catch (IllegalArgumentException | SecurityException e) {
            log.warn("添加媒体链失败: user={}, error={}", operatorOpenId, e.getMessage());
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            log.error("添加媒体链异常", e);
            return ApiResponse.fail("系统繁忙，请稍后再试");
        }
    }

    /**
     * 删除媒体外链
     * 权限：创建者可以删自己的，管理员/超管可以删任何人的(用于治理违规内容)
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> deleteMediaLink(@NotBlank String operatorOpenId,
                                             @NotNull Long mediaLinkId) {
        try {
            UserInfo operator = userRepository.findByOpenid(operatorOpenId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            PerformanceMediaLink mediaLink = mediaLinkRepository.findById(mediaLinkId)
                    .orElseThrow(() -> new IllegalArgumentException("媒体资源不存在"));

            Performance performance = mediaLink.getPerformance();

            if (!hasPermission(operator, performance)) {
                throw new SecurityException("无权删除此资源");
            }

            // 如果是管理员操作，打印一条治理日志
            if (ROLE_ADMIN.equals(operator.getRole()) || ROLE_SUPER_ADMIN.equals(operator.getRole())) {
                log.info("管理员 [{}] 强制下架/删除了违规外链 ID={}, Content={}",
                        operator.getNickname(), mediaLinkId, mediaLink.getExternalKey());
            }

            mediaLinkRepository.delete(mediaLink);
            return ApiResponse.success(null);

        } catch (Exception e) {
            log.error("删除媒体链失败", e);
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 获取某演出的所有外链
     * 修改返回类型为 List<MediaLinkDto>
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<MediaLinkDto>> getMediaLinks(Long performanceId) {
        List<PerformanceMediaLink> links = mediaLinkRepository.findByPerformanceIdOrderBySortOrderDesc(performanceId);

        // 转换 Entity 为 DTO
        List<MediaLinkDto> dtos = links.stream()
                .map(MediaLinkDto::from)
                .collect(Collectors.toList());

        return ApiResponse.success(dtos);
    }

    // ================== 辅助方法 ==================

    /**
     * 统一权限校验逻辑
     * @return true: 有权限 (是管理员 还是 是举办者), false: 无权限
     */
    private boolean hasPermission(UserInfo operator, Performance performance) {
        // 1. 检查是否是管理员/超管
        if (ROLE_ADMIN.equals(operator.getRole()) || ROLE_SUPER_ADMIN.equals(operator.getRole())) {
            return true;
        }

        // 2. 检查是否是演出的组织者
        return checkIsOrganizer(operator, performance);
    }

    private boolean checkIsOrganizer(UserInfo user, Performance performance) {
        String type = performance.getOrganizerType();
        Long organizerId = performance.getOrganizerId();

        if ("USER".equalsIgnoreCase(type)) {
            return organizerId.equals(user.getId()) && Integer.valueOf(1).equals(user.getStatus());
        }

        if ("ORGANIZATION".equalsIgnoreCase(type)) {
            Optional<OrganizationInfo> orgOpt = organizationInfoRepository.findById(organizerId);
            if (orgOpt.isEmpty()) return false;

            OrganizationInfo org = orgOpt.get();
            if (org.getStatus() != 1) return false;
            if (org.getLeader() == null) return false;

            return org.getLeader().getId().equals(user.getId());
        }

        return false;
    }
}