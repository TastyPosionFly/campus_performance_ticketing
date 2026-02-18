package org.example.campus_performance_ticketing.logic;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.dao.VenueBlockedDayRepository;
import org.example.campus_performance_ticketing.dao.VenueOpeningHoursRepository;
import org.example.campus_performance_ticketing.dao.VenueRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.venue.BlockedDateSimple;
import org.example.campus_performance_ticketing.logic.dto.venue.OpeningAndBlockedHoursDto;
import org.example.campus_performance_ticketing.logic.dto.venue.OpeningHoursDto;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.model.Venue;
import org.example.campus_performance_ticketing.model.VenueBlockedDay;
import org.example.campus_performance_ticketing.model.VenueOpeningHours;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor

public class VenueOpeningHoursService {
    private final VenueRepository venueRepository;
    private final VenueOpeningHoursRepository openingHoursRepository;
    private final UserRepository userRepository;
    private final VenueBlockedDayRepository blockedDayRepository;


    /**
     * 设置场地的开放时间 (批量设置)
     * 支持传入部分几天的配置，也可以传入整周配置
     *
     * @param venueId     场地 ID
     * @param dtoList     配置列表
     * @param operatorOpenId 操作人 OpenID
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<Void> setOpeningHours(@NotNull Long venueId,
                                             @Valid List<OpeningHoursDto> dtoList,
                                             @NotBlank String operatorOpenId) {
        // 1. 校验用户
        UserInfo operator = userRepository.findByOpenid(operatorOpenId).orElse(null);
        if (operator == null) {
            // 明确返回 400 或 错误提示
            return ApiResponse.fail("用户不存在");
        }

        // 2. 校验场地
        Venue venue = venueRepository.findById(venueId).orElse(null);
        if (venue == null) {
            return ApiResponse.fail("场地不存在");
        }

        // 3. 权限校验 (逻辑同 VenueService)
        boolean isManager = venue.getManager() != null && venue.getManager().getId().equals(operator.getId());
        boolean isSuperAdmin = "SUPER_ADMIN".equals(operator.getRole());

        if (!isManager && !isSuperAdmin) {
            return ApiResponse.fail("您没有权限设置该场地的开放时间");
        }

        // 4. 遍历处理每一天的配置
        for (OpeningHoursDto dto : dtoList) {
            // 4.1 业务校验
            if (Boolean.FALSE.equals(dto.getIsClosed())) {
                if (dto.getOpenTime() == null || dto.getCloseTime() == null) {
                    return ApiResponse.fail("星期" + dto.getDayOfWeek() + "设为开放时，必须填写开始和结束时间");
                }
                if (dto.getOpenTime().isAfter(dto.getCloseTime())) {
                    return ApiResponse.fail("星期" + dto.getDayOfWeek() + "的时间设置无效：结束时间不能早于开始时间");
                }
            } else {
                // 如果是关闭状态，设置默认时间防止空指针（可选，看数据库是否允许null，这里实体类定义为 nullable=false）
                if (dto.getOpenTime() == null) dto.setOpenTime(LocalTime.of(0, 0));
                if (dto.getCloseTime() == null) dto.setCloseTime(LocalTime.of(0, 0));
            }

            // 4.2 查找数据库中是否已有该天配置 (Upsert 逻辑)
            VenueOpeningHours openingHours = openingHoursRepository
                    .findByVenueIdAndDayOfWeek(venueId, dto.getDayOfWeek())
                    .orElse(new VenueOpeningHours());

            // 4.3 设值
            openingHours.setVenue(venue);
            openingHours.setDayOfWeek(dto.getDayOfWeek());
            openingHours.setOpenTime(dto.getOpenTime());
            openingHours.setCloseTime(dto.getCloseTime());
            openingHours.setIsClosed(dto.getIsClosed() != null && dto.getIsClosed());

            // 4.4 保存
            openingHoursRepository.save(openingHours);
        }

        return ApiResponse.success(null);
    }



    /**
     * 查询指定场馆的开放时间和屏蔽时间（包含屏蔽原因）
     *
     * @param venueId 场馆 ID
     * @return 每天的开放时间 + 被屏蔽日期（每个仅包含 date + reason）
     */
    public ApiResponse<OpeningAndBlockedHoursDto> getVenueHoursAndBlockedDates(@NotNull Long venueId) {
        // 1. 查询并格式化开放时间（保持或替换为你现有的逻辑）
        List<OpeningHoursDto> openingHours = getFormattedOpeningHours(venueId);

        // 2. 查询屏蔽时间并映射（假设 VenueBlockedDay 有 getBlockedDate() 和 getReason()）
        List<VenueBlockedDay> raw = blockedDayRepository.findByVenueId(venueId);

        List<BlockedDateSimple> blockedDates = raw.stream()
                .map(b -> new BlockedDateSimple(
                        b.getBlockedDate(),
                        b.getReason()
                ))
                .collect(Collectors.toList());

        // 3. 封装返回
        OpeningAndBlockedHoursDto result = new OpeningAndBlockedHoursDto(openingHours, blockedDates);
        return ApiResponse.success(result);
    }

    /**
     * 查询某场地在指定星期几的开放时间（返回 DTO，若未配置则返回 null）
     * @param venueId 场地ID
     * @param dayOfWeek 星期 (1-7)
     * @return OpeningHoursDto or null
     */
    public OpeningHoursDto getOpeningHoursForDay(@NotNull Long venueId, @NotNull Integer dayOfWeek) {
        return openingHoursRepository.findByVenueIdAndDayOfWeek(venueId, dayOfWeek)
                .map(this::convertToDto)
                .orElse(null);
    }

    /**
     * DTO 转换辅助方法
     */
    private OpeningHoursDto convertToDto(VenueOpeningHours entity) {
        OpeningHoursDto dto = new OpeningHoursDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * 私有方法：查询并格式化开放时间
     */
    private List<OpeningHoursDto> getFormattedOpeningHours(@NotNull Long venueId) {
        List<VenueOpeningHours> hoursList = openingHoursRepository.findByVenueId(venueId);

        return hoursList.stream()
                .sorted(Comparator.comparingInt(VenueOpeningHours::getDayOfWeek)) // 按星期几排序
                .map(hours -> new OpeningHoursDto(
                        hours.getDayOfWeek(),
                        hours.getIsClosed(),// 是否闭馆
                        hours.getOpenTime(), // 开馆时间
                        hours.getCloseTime() // 闭馆时间
                ))
                .collect(Collectors.toList());
    }

    /**
     * 检查某场地在指定日期是否被屏蔽
     * @param venueId 场地ID
     * @param date 日期
     * @return true 表示被屏蔽
     */
    public boolean isBlockedOnDate(@NotNull Long venueId, @NotNull java.time.LocalDate date) {
        return blockedDayRepository.existsByVenueIdAndBlockedDate(venueId, date);
    }

    /**
     * 批量获取多场馆在指定日期的开放时段和屏蔽标记
     * 返回值：
     * - 第一个 Map: venueId -> OpeningHoursDto (若无配置则不包含或为 null)
     * - 第二个 Map: venueId -> Boolean (是否屏蔽)
     */
    public java.util.Map<String, java.util.Map<Long, Object>> getBulkTodayInfo(List<Long> venueIds, @NotNull java.time.LocalDate date) {
        java.util.Map<Long, OpeningHoursDto> hoursMap = new java.util.HashMap<>();
        java.util.Map<Long, Boolean> blockedMap = new java.util.HashMap<>();

        if (venueIds == null || venueIds.isEmpty()) {
            java.util.Map<String, java.util.Map<Long, Object>> result = new java.util.HashMap<>();
            result.put("hours", new java.util.HashMap<>());
            result.put("blocked", new java.util.HashMap<>());
            return result;
        }

        // 批量查询开放时间配置
        List<VenueOpeningHours> all = openingHoursRepository.findByVenueIdIn(venueIds);
        for (VenueOpeningHours e : all) {
            if (e == null) continue;
            if (e.getDayOfWeek() != null && e.getDayOfWeek().intValue() == date.getDayOfWeek().getValue()) {
                hoursMap.put(e.getVenue().getId(), convertToDto(e));
            }
        }

        // 批量查询屏蔽记录（按给定日期和 venueId 列表）
        List<org.example.campus_performance_ticketing.model.VenueBlockedDay> blocks = blockedDayRepository.findByVenueIdInAndBlockedDate(venueIds, date);
        if (blocks != null) {
            for (org.example.campus_performance_ticketing.model.VenueBlockedDay bd : blocks) {
                blockedMap.put(bd.getVenue().getId(), true);
            }
        }
        // 对未出现的 venueId 标记 false
        for (Long vid : venueIds) {
            blockedMap.putIfAbsent(vid, false);
        }

        java.util.Map<String, java.util.Map<Long, Object>> result = new java.util.HashMap<>();
        java.util.Map<Long, Object> hoursObjectMap = new java.util.HashMap<>();
        for (java.util.Map.Entry<Long, OpeningHoursDto> e : hoursMap.entrySet()) {
            hoursObjectMap.put(e.getKey(), e.getValue());
        }
        java.util.Map<Long, Object> blockedObjectMap = new java.util.HashMap<>();
        for (java.util.Map.Entry<Long, Boolean> e : blockedMap.entrySet()) {
            blockedObjectMap.put(e.getKey(), e.getValue());
        }
        result.put("hours", hoursObjectMap);
        result.put("blocked", blockedObjectMap);
        return result;
    }

}
