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
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@Validated
@RequiredArgsConstructor

public class VenueOpeningHoursService {
    private final VenueRepository venueRepository;
    private final VenueOpeningHoursRepository openingHoursRepository;
    private final UserRepository userRepository;
    private final VenueBlockedDayRepository blockedDayRepository;

    private static final Logger logger = Logger.getLogger(VenueOpeningHoursService.class.getName());

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
     * 查询指定场馆的开放时间和屏蔽时间
     *
     * @param venueId 场馆 ID
     * @return 开放时间和屏蔽时间
     */
    public ApiResponse<OpeningAndBlockedHoursDto> getVenueHoursAndBlockedDates(@NotNull Long venueId) {
        // 1. 查询开放时间
        List<OpeningHoursDto> openingHours = getFormattedOpeningHours(venueId);

        // 2. 查询屏蔽时间
        List<LocalDate> blockedDates = blockedDayRepository.findByVenueId(venueId).stream()
                .map(VenueBlockedDay::getBlockedDate)
                .collect(Collectors.toList());

        // 3. 封装结果
        OpeningAndBlockedHoursDto result = new OpeningAndBlockedHoursDto(openingHours, blockedDates);
        return ApiResponse.success(result);
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
                        hours.getCloseTime(), // 闭馆时间
                        hours.getOpenTime() // 开馆时间
                ))
                .collect(Collectors.toList());
    }

}
