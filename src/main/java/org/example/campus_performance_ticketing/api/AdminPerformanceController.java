package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.logic.AdminPerformanceService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.performance.AdminPerformanceDto;
import org.example.campus_performance_ticketing.logic.dto.performance.CreatePerformanceCmd;
import org.example.campus_performance_ticketing.model.PerformanceSession;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/performance")
@RequiredArgsConstructor
@Validated
public class AdminPerformanceController {

    private final AdminPerformanceService adminPerformanceService;

    /**
     * 管理员强制征用场地并创建演出 (包含图片上传)
     */
    @PostMapping(value = "/preempt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AdminPerformanceDto> preemptAndCreate(
            HttpServletRequest request,
            @RequestPart("data") @Valid CreatePerformanceCmd data) {
        String openId = (String) request.getAttribute("openid");
        return adminPerformanceService.preemptVenueAndCreate(openId, data);
    }
}