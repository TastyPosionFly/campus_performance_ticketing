package org.example.campus_performance_ticketing.logic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.PerformanceRecommendationRepository;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance_recommendation.CreateRecommendationCmd;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.PerformanceRecommendation;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Valid
@RequiredArgsConstructor
public class PerformanceRecommendationService {

    private final PerformanceRecommendationRepository recommendationRepository;
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> addRecommendation(@NotBlank String operatorOpenId,
                                               @Valid CreateRecommendationCmd cmd) {
        try {
            validateAdminPermission(operatorOpenId);

            // 【核心修改】校验逻辑变更
            // 检查：该演出 + 该类型 是否已存在
            if (recommendationRepository.existsByPerformanceIdAndType(cmd.getPerformanceId(), cmd.getType())) {
                throw new IllegalArgumentException("该演出已在当前推荐分类中，请勿重复添加。");
            }

            Performance performance = performanceRepository.findById(cmd.getPerformanceId())
                    .orElseThrow(() -> new IllegalArgumentException("指定的演出不存在"));

            if (Integer.valueOf(1).equals(performance.getPublishStatus()) == false) {
                throw new IllegalArgumentException("无法推荐未发布的演出");
            }

            if (cmd.getStartTime() != null && cmd.getEndTime() != null) {
                if (cmd.getEndTime().isBefore(cmd.getStartTime())) {
                    throw new IllegalArgumentException("结束时间不能早于开始时间");
                }
            }

            PerformanceRecommendation rec = new PerformanceRecommendation();
            rec.setPerformance(performance);
            rec.setType(cmd.getType());
            rec.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
            rec.setStartTime(cmd.getStartTime());
            rec.setEndTime(cmd.getEndTime());

            recommendationRepository.save(rec);

            log.info("管理员[{}]添加了推荐位: 演出ID={}, 类型={}", operatorOpenId, performance.getId(), cmd.getType());
            return ApiResponse.success(null);

        } catch (IllegalArgumentException | SecurityException e) {
            log.warn("添加推荐失败: {}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        } catch (Exception e) {
            log.error("添加推荐异常", e);
            return ApiResponse.fail("系统繁忙，请稍后再试");
        }
    }

    // ... 其他方法保持不变 (deleteRecommendation, getActiveRecommendations 等) ...
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> deleteRecommendation(@NotBlank String operatorOpenId, @NotNull Long recommendationId) {
        try {
            validateAdminPermission(operatorOpenId);
            if (!recommendationRepository.existsById(recommendationId)) {
                throw new IllegalArgumentException("推荐记录不存在");
            }
            recommendationRepository.deleteById(recommendationId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PerformanceRecommendation>> getActiveRecommendations(@NotNull Integer type) {
        return ApiResponse.success(recommendationRepository.findActiveRecommendations(type, LocalDateTime.now()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<PerformanceRecommendation>> getAllRecommendationsConfig(@NotBlank String operatorOpenId, @NotNull Integer type) {
        try {
            validateAdminPermission(operatorOpenId);
            return ApiResponse.success(recommendationRepository.findByTypeOrderByCreateTimeDesc(type));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    private void validateAdminPermission(String openId) {
        UserInfo operator = userRepository.findByOpenid(openId)
                .orElseThrow(() -> new IllegalArgumentException("管理员用户不存在"));
        if (!ROLE_ADMIN.equals(operator.getRole()) && !ROLE_SUPER_ADMIN.equals(operator.getRole())) {
            throw new SecurityException("无权操作");
        }
        if (operator.getStatus() != 1) {
            throw new SecurityException("管理员账号状态异常");
        }
    }
}