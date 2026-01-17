package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.campus_performance_ticketing.logic.ApplicationService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationPublicDto;
import org.example.campus_performance_ticketing.logic.dto.application.PendingApplicationDto;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationAuditCommand;
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
     * GET /api/application/my-applications?applicationType=CREATE_ORG
     */
    @GetMapping("/my-applications")
    public ApiResponse<List<ApplicationPublicDto>> getUserApplications(
            HttpServletRequest request,
            @RequestParam(required = false) String applicationType
    ) {
        String openId = (String) request.getAttribute("openid");
        return applicationService.getUserApplications(openId, applicationType);
    }

    /**
     * 查询所有申请（可筛选类型、状态，需管理员权限）
     * GET /api/application/list?applicationType=CREATE_ORG&status=1
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
     * 批量审核申请（POST，body为List<ApplicationAuditCommand>）
     * POST /api/application/batch-review
     * [
     *   {"applicationId":101,"newStatus":2,"reason":"同意理由"},
     *   {"applicationId":102,"newStatus":3,"reason":"拒绝理由"}
     * ]
     */
    @PostMapping("/batch-review")
    public ApiResponse<Void> batchChangeApplicationStatus(
            HttpServletRequest request,
            @RequestBody List<ApplicationAuditCommand> audits
    ) {
        String openId = (String) request.getAttribute("openid");
        return applicationService.batchChangeApplicationStatus(openId, audits);
    }

    /**
     * 撤销个人申请（POST /api/application/revoke?applicationId=678）
     */
    @PostMapping("/revoke")
    public ApiResponse<Void> revokeCreateOrganizationApplication(
            HttpServletRequest request,
            @RequestParam Long applicationId
    ) {
        String openId = (String) request.getAttribute("openid");
        return applicationService.revokeCreateApplication(openId, applicationId);
    }
}