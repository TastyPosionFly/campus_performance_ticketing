package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.campus_performance_ticketing.logic.ApplicationService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.ApplicationPublicDto;
import org.example.campus_performance_ticketing.logic.dto.PendingApplicationDto;
import org.example.campus_performance_ticketing.logic.dto.ApplicationAuditCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 申请相关接口
 */
@RestController
@RequestMapping("/api/application")
public class ApplicationController {

    private final ApplicationService applicationService;

    @Autowired
    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    /**
     * 查询本人提交的申请记录
     */
    @GetMapping("/my-applications")
    public ApiResponse<List<ApplicationPublicDto>> getUserApplications(HttpServletRequest request) {
        String openId = (String) request.getAttribute("openid");
        return applicationService.getUserApplications(openId);
    }

    /**
     * 查看申请列表（可筛选类型、状态，管理员权限）
     * @param applicationType 申请类型（可选，如"CREATE_ORG"）
     * @param status 申请状态（可选，如1-待审核,2-通过...）
     */
    @GetMapping("/list")
    public ApiResponse<List<PendingApplicationDto>> listApplications(
            HttpServletRequest request,
            @RequestParam(required = false) String applicationType,
            @RequestParam(required = false) Integer status
    ) {
        String openId = (String) request.getAttribute("openid");
        return applicationService.listApplications(openId, applicationType, status);
    }

    /**
     * 批量审核申请（管理员权限）
     * 推荐POST body json：[{"applicationId":101, "newStatus":2, "reason":"同意理由"}]
     */
    @PostMapping("/batch-review")
    public ApiResponse<Void> batchChangeApplicationStatus(
            HttpServletRequest request,
            @RequestBody List<ApplicationAuditCommand> audits
    ) {
        String openId = (String) request.getAttribute("openid");
        return applicationService.batchChangeApplicationStatus(openId, audits);
    }

}