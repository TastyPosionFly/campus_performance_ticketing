package org.example.campus_performance_ticketing.logic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.dao.PerformanceSessionRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.AdminPerformanceDto;
import org.example.campus_performance_ticketing.logic.dto.performance.CreatePerformanceCmd;
import org.example.campus_performance_ticketing.logic.dto.performance.SessionCmd;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceSession;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 管理员专属演出服务：处理强制征用、批量调度等高级功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Valid
public class AdminPerformanceService {

    private final PerformanceService performanceService; // 复用基础创建逻辑
    private final PerformanceSessionRepository sessionRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;

    // 假设你有通知服务
    // private final NotificationService notificationService;

    /**
     * 强制征用场地并创建演出
     * @param adminOpenId 管理员用户的 OpenID
     * cmd 创建演出的命令对象
     * @return 创建成功的演出 DTO 包装在 ApiResponse 中
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<AdminPerformanceDto> preemptVenueAndCreate(
            @NotBlank String adminOpenId,
            @Valid CreatePerformanceCmd cmd) {

        try {
            UserInfo adminUser = userRepository.findByOpenid(adminOpenId)
                    .orElseThrow(() -> new IllegalArgumentException("管理员用户不存在"));

            // 1. 权限校验
            if (!"ADMIN".equals(adminUser.getRole()) && !"SUPER_ADMIN".equals(adminUser.getRole())) {
                throw new SecurityException("权限不足: " + adminUser.getRole());
            }

            // 2. 校验主办方是否存在
            validateOrganizerExistence(cmd.getOrganizerType(), cmd.getOrganizerId());

            // 3. 执行强制征用逻辑
            int conflictCount = 0; // 定义冲突计数器

            if (cmd.getSessions() != null) {
                for (SessionCmd sessionCmd : cmd.getSessions()) {
                    List<PerformanceSession> conflicts = sessionRepository.findConflicts(
                            sessionCmd.getVenueId(),
                            sessionCmd.getStartTime(),
                            sessionCmd.getEndTime()
                    );

                    for (PerformanceSession conflictSession : conflicts) {
                        Performance oldPerformance = conflictSession.getPerformance();

                        // 已经被征用的不再重复处理
                        if (oldPerformance.getPublishStatus() == 6) continue;
                        // 已取消的也不处理（根据您的业务，如果 status=3 是已取消，这里可以跳过）
                        if (oldPerformance.getPublishStatus() == 3) continue;

                        // 状态改为 6-被征用
                        oldPerformance.setPublishStatus(6);
                        performanceRepository.save(oldPerformance);

                        conflictCount++; // 计数+1

                        log.warn("管理员[{}]强制征用场地，挤掉了原有演出[ID={}, Title={}, Organizer={}]",
                                adminOpenId,
                                oldPerformance.getId(),
                                oldPerformance.getTitle());
                    }
                }
            }

            // 4. 创建新演出
            // 注意：请确保 PerformanceService 中已经添加了 createPerformanceEntity 方法
            Performance newPerformance = performanceService.createPerformanceEntity(cmd);

            // 5. 转换为 DTO 返回
            AdminPerformanceDto resultDto = AdminPerformanceDto.from(newPerformance);

            ApiResponse<AdminPerformanceDto> response = ApiResponse.success(resultDto);
            if (conflictCount > 0) {
                response.setMessage("强制征用成功，已自动取消 " + conflictCount + " 个冲突演出");
            }
            return response;

        } catch (Exception e) {
            log.error("强制征用并创建演出失败", e);
            // 这里建议抛出运行时异常，让 GlobalExceptionHandler 处理，或者像这样手动捕获
            if (e instanceof SecurityException) {
                throw (SecurityException) e; // 保持 403 状态码
            }
            return ApiResponse.fail("强制征用并创建演出失败：" + e.getMessage());
        }
    }

    private Long resolveReceiverId(Performance p) {
        // 简单的辅助逻辑...
        if ("USER".equals(p.getOrganizerType())) return p.getOrganizerId();
        // else return orgService.getLeaderId(p.getOrganizerId());
        return null; // 仅示例
    }

    /**
     * 辅助方法：仅校验ID是否存在，不校验权限
     */
    private void validateOrganizerExistence(String type, Long id) {
        if ("ORGANIZATION".equals(type)) {
            boolean exists = organizationInfoRepository.existsById(id);
            if (!exists) {
                throw new IllegalArgumentException("指定的组织/社团 ID=" + id + " 不存在");
            }
        } else if ("USER".equals(type)) {
            boolean exists = userRepository.existsById(id);
            if (!exists) {
                throw new IllegalArgumentException("指定的用户 ID=" + id + " 不存在");
            }
        } else {
            throw new IllegalArgumentException("未知的组织类型: " + type);
        }
    }
}