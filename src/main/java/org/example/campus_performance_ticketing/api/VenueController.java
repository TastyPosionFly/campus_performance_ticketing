package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.example.campus_performance_ticketing.logic.VenueBlockDayService;
import org.example.campus_performance_ticketing.logic.VenueOpeningHoursService;
import org.example.campus_performance_ticketing.logic.VenueService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.venue.*;
import org.example.campus_performance_ticketing.model.VenueBlockedDay;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;
    private final VenueOpeningHoursService venueOpeningHoursService;
    private final VenueBlockDayService venueBlockDayService;

    public VenueController(VenueService venueService,
                           VenueOpeningHoursService venueOpeningHoursService,
                           VenueBlockDayService venueBlockDayService) {
        this.venueBlockDayService = venueBlockDayService;
        this.venueOpeningHoursService = venueOpeningHoursService;
        this.venueService = venueService;
    }

    /**
     * 创建场地
     *
     * @param dto 前端表单数据 (包含文件和普通字段)
     *            注意：前端需要使用 FormData 发送请求
     *            Content-Type: multipart/form-data
     * @param  request HttpServletRequest 对象，用于获取拦截器中设置的 openId
     * @return 响应结果
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> createVenue(
            @Valid @ModelAttribute CreateVenueDto dto,
            HttpServletRequest request
    ) {
        String openId = (String) request.getAttribute("openid");
        // 调用 Service
        return venueService.createVenue(dto, openId);
    }

    /**
     * 获取场地详情
     * GET /api/venues/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<VenueDetailDto> getVenueDetail(@PathVariable @NotNull Long id) {
        return venueService.getVenueDetail(id);
    }

    /**
     * 查询场地列表
     *
     * URL: GET /api/venues
     *
     * 场景说明:
     * 1. 查询所有: /api/venues (不带参数)
     * 2. 搜索名称: /api/venues?name=大剧院
     * 3. 筛选类型: /api/venues?type=1
     * 4. 组合筛选: /api/venues?type=1&status=1
     */
    @GetMapping
    public ApiResponse<List<VenueDetailDto>> getVenueList(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status
    ) {
        // 调用 Service 层的搜索方法
        // 该方法返回的 DTO 已经包含了拼接好的完整图片 URL (封面、轮播图、管理员头像)
        return venueService.searchVenues(name, type, status);
    }


    @GetMapping("/{venueId}/events")
    public ApiResponse<List<VenueEventDto>> getVenueEvents(
            @PathVariable Long venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String end
    ) {
        try {
            LocalDate startDate = LocalDate.parse(start);
            LocalDate endDate = LocalDate.parse(end);
            List<VenueEventDto> events = venueService.getEventsForVenue(venueId, startDate, endDate);
            return ApiResponse.success(events);
        } catch (java.time.format.DateTimeParseException ex) {
            return ApiResponse.fail("日期格式错误，请使用 yyyy-MM-dd");
        } catch (IllegalArgumentException ex) {
            return ApiResponse.fail(ex.getMessage());
        } catch (Exception ex) {
            return ApiResponse.fail("获取场地日程失败: " + ex.getMessage());
        }
    }

    /**
     * 更新场地信息
     */
    @PostMapping("/update") // 或者 @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updateVenue(
            @Valid @ModelAttribute UpdateVenueDto dto,
            HttpServletRequest request
    ) {
        String openId = (String) request.getAttribute("openid");
        return venueService.updateVenueBasicInfo(dto, openId);
    }


    /**
     * 删除场地
     */
    @DeleteMapping("/{id}/delete")
    public ApiResponse<Void> deleteVenue(@PathVariable Long id,
                                         HttpServletRequest request) {
        String openId = (String) request.getAttribute("openid");
        return venueService.deleteVenue(id, openId);
    }


    /**
     * 设置开放时间 (批量)
     * POST /api/venues/{venueId}/hours
     */
    @PostMapping("/{venueId}/hours")
    public ApiResponse<Void> setOpeningHours(
            @PathVariable @NotNull Long venueId,
            @RequestBody @Valid List<OpeningHoursDto> dtoList,
            HttpServletRequest request
    ) {
        String openId = (String) request.getAttribute("openid");
        return venueOpeningHoursService.setOpeningHours(venueId, dtoList, openId);
    }

    /**
     * 屏蔽场馆并取消当天演出
     *
     * @param request  HTTP 请求对象
     * @param requestDto 屏蔽场馆的请求数据
     * @return 屏蔽结果响应
     */
    @PostMapping(value = "/block", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<BlockVenueResponseDto> blockVenueAndCancelPerformances(
            HttpServletRequest request,
            @Valid @RequestBody BlockVenueRequestDto requestDto) {
        String openId = (String) request.getAttribute("openid");
        // 调用服务层逻辑处理并返回结果
        return venueBlockDayService.blockVenueAndCancelPerformances(openId, requestDto);
    }

    /**
     * 获取场馆的开放时间和屏蔽日期
     *
     * @param venueId 场馆 ID
     * @return 每天的开放时间 + 屏蔽日期
     */
    @GetMapping("/{venueId}/hours-and-blocks")
    public ApiResponse<OpeningAndBlockedHoursDto> getVenueHoursAndBlockedDates(
            @PathVariable @NotNull Long venueId) {
        return venueOpeningHoursService.getVenueHoursAndBlockedDates(venueId);
    }

}