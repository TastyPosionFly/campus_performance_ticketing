package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.logic.PerformanceApplyService;
import org.example.campus_performance_ticketing.logic.PerformanceSearchService;
import org.example.campus_performance_ticketing.logic.PerformanceUpdateService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.CreatePerformanceCmd;
import org.example.campus_performance_ticketing.logic.dto.performance.PerformanceDetailDto;
import org.example.campus_performance_ticketing.logic.dto.performance.UpdatePerformanceRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Validated
public class PerformanceController {

    private final PerformanceApplyService performanceApplyService;
    private final PerformanceUpdateService performanceUpdateService;
    private final PerformanceSearchService performanceSearchService;

    /**
     * 【方案：数据 JSON 提交，文件单独上传】
     *
     * 1) 前端先 POST /api/performance/apply (application/json)
     *    Body = CreatePerformanceCmd
     *    返回 { data: { id: performanceId } }
     *
     * 2) 前端再上传海报
     *    POST /api/performance/{id}/poster (multipart/form-data) part name=poster
     *
     * 3) 前端逐张上传演职人员照片
     *    POST /api/performance/{id}/staff-photos (multipart/form-data) part name=staffPhotos
     */
    @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Map<String, Object>> submitApplicationJson(
            HttpServletRequest request,
            @RequestBody @Valid CreatePerformanceCmd data
    ) {
        String openId = (String) request.getAttribute("openid");
        Long performanceId = performanceApplyService.createApplicationDraftReturnId(openId, data);
        return ApiResponse.success(Map.of("id", performanceId));
    }

    /**
     * 上传演职人员照片（单独上传，前端逐张上传也可）
     */
    @PostMapping(value = "/{id}/staff-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> uploadStaffPhoto(
            HttpServletRequest request,
            @PathVariable("id") Long performanceId,
            @RequestPart("staffPhotos") MultipartFile staffPhoto
    ) {
        String openId = (String) request.getAttribute("openid");
        performanceApplyService.appendStaffPhoto(openId, performanceId, staffPhoto);
        return ApiResponse.success(null);
    }


    /**
     * 更新演出信息（JSON 提交）
     * @param request
     * @param updateRequest
     * @return
     */
    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<PerformanceDetailDto> updatePerformanceJson(
            HttpServletRequest request,
            @RequestBody @Valid UpdatePerformanceRequestDto updateRequest
    ) {
        String userOpenId = (String) request.getAttribute("openid");
        return performanceUpdateService.updatePerformance(userOpenId, updateRequest, null, null);
    }

    /**
     * 更新海报（单独上传）
     * @param request
     * @param performanceId
     * @param poster
     * @return
     */
    @PostMapping(value = "/{id}/poster", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updatePoster(
            HttpServletRequest request,
            @PathVariable("id") Long performanceId,
            @RequestPart("poster") MultipartFile poster
    ) {
        String openId = (String) request.getAttribute("openid");
        performanceUpdateService.updatePosterOnly(openId, performanceId, poster);
        return ApiResponse.success(null);
    }

    /**
     * 更新演职人员照片（单独上传，前端逐张上传也可）
     * @param request
     * @param performanceId
     * @param staffId
     * @param avatar
     * @return
     */
    @PostMapping(value = "/{id}/staff/{staffId}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> updateStaffAvatar(
            HttpServletRequest request,
            @PathVariable("id") Long performanceId,
            @PathVariable("staffId") Long staffId,
            @RequestPart("avatar") MultipartFile avatar
    ) {
        String openId = (String) request.getAttribute("openid");
        performanceUpdateService.updateStaffAvatarOnly(openId, performanceId, staffId, avatar);
        return ApiResponse.success(null);
    }

    /**
     * 分页搜索演出列表
     * GET /api/performance/list?keyword=xxx&categoryId=1&status=1&page=0&size=10
     *
     * @param keyword    搜索关键词 (可选)
     * @param categoryId 分类ID (可选)
     * @param status     状态 (可选，默认建议传 1-已发布)
     * @param venueName  场地名称（模糊匹配，选填）
     * @param page       页码 (默认 0)
     * @param size       每页大小 (默认 10)
     */
    @GetMapping("/list")
    public ApiResponse<Page<PerformanceDetailDto>> searchPerformances(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String venueName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return performanceSearchService.searchPerformances(keyword, categoryId, status, venueName, page, size);
    }

    /**
     * 获取演出详情
     * GET /api/performance/{id}
     *
     * @param id 演出 ID
     */
    @GetMapping("/{id}")
    public ApiResponse<PerformanceDetailDto> getPerformanceDetail(@PathVariable Long id) {
        return performanceSearchService.getPerformanceDetail(id);
    }

    /**
     * 获取演出小程序码（二维码）
     * @param request
     * @param response
     * @param id
     * @throws Exception
     */
    @GetMapping(value = "/{id}/wxacode", produces = MediaType.IMAGE_PNG_VALUE)
    public void getPerformanceWxaCode(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable Long id
    ) throws Exception {
        // 详情页需要登录：生成二维码也建议要求登录（至少要求已登录用户）
        String openId = (String) request.getAttribute("openid");
        if (openId == null || openId.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return;
        }
        performanceSearchService.writePerformanceWxaCodeToResponse(id, response);
    }
}