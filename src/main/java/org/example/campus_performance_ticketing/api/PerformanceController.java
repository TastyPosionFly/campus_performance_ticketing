package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
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
     * 用户/社团提交演出申请 (包含图片上传)
     * Content-Type: multipart/form-data
     *
     * @param data JSON数据，包含演出的详细信息
     * @param poster 海报图片文件
     * @param staffPhotos 演职人员照片文件列表
     */
    @PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Void> submitApplication(
            HttpServletRequest request,
            @RequestPart("data") @Valid CreatePerformanceCmd data,
            @RequestPart(value = "poster", required = false) MultipartFile poster,
            @RequestPart(value = "staffPhotos", required = false) List<MultipartFile> staffPhotos) {

        String openId = (String) request.getAttribute("openid");
        return performanceApplyService.submitPerformanceApplication(openId, data, poster, staffPhotos);
    }


    /**
     * 修改演出，包括标题、描述、状态、场次等，支持上传海报图片和演职人员定妆照。
     *
     * @param request HTTP请求，包含用户的 openid 属性
     * @param updateRequest 包含要更新的演出信息
     *@param newPosterFile 可选的新海报图片文件
     *@param staffPhotoFiles 可选的演职人员定妆照文件列表
     *@return 更新后的演出详情
     */
    @PutMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PerformanceDetailDto> updatePerformance(
            HttpServletRequest request,
            @RequestPart("data") @Valid UpdatePerformanceRequestDto updateRequest, // 改为 RequestPart
            @RequestPart(value = "newPosterFile", required = false) MultipartFile newPosterFile,
            @RequestPart(value = "staffPhotoFiles", required = false) List<MultipartFile> staffPhotoFiles) { // 改为 List

        String userOpenId = (String) request.getAttribute("openid");
        return performanceUpdateService.updatePerformance(userOpenId, updateRequest, newPosterFile, staffPhotoFiles);
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
}