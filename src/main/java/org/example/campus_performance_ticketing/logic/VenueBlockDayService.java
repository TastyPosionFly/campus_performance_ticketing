package org.example.campus_performance_ticketing.logic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.campus_performance_ticketing.dao.PerformanceSessionRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.dao.VenueBlockedDayRepository;
import org.example.campus_performance_ticketing.dao.VenueRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.venue.BlockVenueRequestDto;
import org.example.campus_performance_ticketing.logic.dto.venue.BlockVenueResponseDto;
import org.example.campus_performance_ticketing.model.PerformanceSession;
import org.example.campus_performance_ticketing.model.Venue;
import org.example.campus_performance_ticketing.model.VenueBlockedDay;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 服务层：处理场馆屏蔽及取消演出逻辑
 */
@Slf4j
@Service
@Valid
@RequiredArgsConstructor
public class VenueBlockDayService {

    private final VenueBlockedDayRepository venueBlockedDayRepository;
    private final PerformanceSessionRepository performanceSessionRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    /**
     * 屏蔽场馆并取消当天演出，返回封装的响应 DTO
     *
     * @param openId 用户 OpenId
     * @param requestDto 请求 DTO
     * @return 屏蔽结果的响应 DTO
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<BlockVenueResponseDto> blockVenueAndCancelPerformances(
            @NotBlank String openId,
            @Valid BlockVenueRequestDto requestDto) {

        try {
            // 验证用户权限
            UserInfo userInfo = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            if (!"VENUE_ADMIN".equals(userInfo.getRole()) && !"SUPER_ADMIN".equals(userInfo.getRole())) {
                throw new SecurityException("用户权限不足");
            }

            Long venueId = requestDto.getVenueId();
            String reason = requestDto.getReason();

            // 兼容：单日 + 多日 -> 统一成列表
            List<LocalDate> blockedDates = requestDto.resolveBlockedDates();
            if (blockedDates == null || blockedDates.isEmpty()) {
                throw new IllegalArgumentException("blockedDate 或 blockedDates 至少需要提供一个日期");
            }

            // 查找场馆是否存在
            Venue venue = venueRepository.findById(venueId)
                    .orElseThrow(() -> new IllegalArgumentException("场馆不存在"));

            int newlyBlockedCount = 0;
            int affectedSessionCount = 0;

            for (LocalDate blockedDate : blockedDates) {
                // 验证日期合法性
                if (blockedDate.isBefore(LocalDate.now())) {
                    // 兼容批量：建议跳过过去日期（或改成直接 throw 终止）
                    log.warn("跳过过去日期: {}", blockedDate);
                    throw new IllegalArgumentException("无法屏蔽过去的日期: " + blockedDate);
                }

                // 检查是否已屏蔽
                if (venueBlockedDayRepository.existsByVenueIdAndBlockedDate(venueId, blockedDate)) {
                    log.info("该日期已被屏蔽，跳过: venueId={}, date={}", venueId, blockedDate);
                    continue;
                }

                // 保存屏蔽记录
                VenueBlockedDay blockedDay = new VenueBlockedDay();
                blockedDay.setVenue(venue);
                blockedDay.setBlockedDate(blockedDate);
                blockedDay.setReason(reason);
                blockedDay.setCreator(userInfo);
                venueBlockedDayRepository.save(blockedDay);
                newlyBlockedCount++;

                log.info("场馆 (ID: {}) 已屏蔽日期: {}, 理由: {}", venueId, blockedDate, reason);

                // 找到并取消当天场馆的所有演出
                List<PerformanceSession> sessions = performanceSessionRepository.findByVenueIdAndStartTimeBetween(
                        venueId, blockedDate.atStartOfDay(), blockedDate.plusDays(1).atStartOfDay()
                );

                for (PerformanceSession session : sessions) {
                    session.getPerformance().setPublishStatus(6); // 你当前的“取消”语义
                    performanceSessionRepository.save(session);
                    affectedSessionCount++;
                    log.info("场馆 (ID: {}) 的演出 (ID: {}, 标题: {}) 已取消，日期: {}",
                            venueId, session.getPerformance().getId(), session.getPerformance().getTitle(), session.getStartTime());
                }
            }

            // 返回响应 DTO（你现有 DTO 是单日期结构，这里用“第一个日期/或null”兼容返回）
            LocalDate firstDate = blockedDates.get(0);

            BlockVenueResponseDto blockVenueResponseDto = new BlockVenueResponseDto(
                    venueId,
                    firstDate, // 兼容旧字段：这里返回首个日期（或你也可以传 null）
                    reason,
                    affectedSessionCount,
                    String.format("批量操作完成：新增屏蔽 %d 天，影响场次 %d 个", newlyBlockedCount, affectedSessionCount)
            );

            ApiResponse<BlockVenueResponseDto> response = ApiResponse.success(blockVenueResponseDto);
            response.setMessage("操作成功");
            return response;

        } catch (Exception e) {
            log.error("屏蔽场馆并取消演出时发生错误: {}", e.getMessage());
            return ApiResponse.fail("操作失败: " + e.getMessage());
        }
    }
}