package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.logic.PerformanceService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.CreatePerformanceCmd;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Validated
public class PerformanceController {

    private final PerformanceService performanceService;

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
        return performanceService.submitPerformanceApplication(openId, data, poster, staffPhotos);
    }
}