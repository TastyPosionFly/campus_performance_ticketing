package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.campus_performance_ticketing.logic.OrganizationService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.ApplyOrganizationRequest;
import org.example.campus_performance_ticketing.model.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 组织相关接口
 */
@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationService organizationService;

    @Autowired
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * 申请创建组织
     */
    @PostMapping("/apply")
    public ApiResponse<Void> applyCreateOrganization(
            HttpServletRequest request,
            @RequestBody ApplyOrganizationRequest body) {
        String openId = (String) request.getAttribute("openid");
        return organizationService.applyCreateOrganization(openId, body.getOrgName(), body.getOrgDescription());
    }


    /**
     * 撤销组织创建申请
     */
    @PostMapping("/revoke")
    public ApiResponse<Void> revokeCreateOrganizationApplication(
            HttpServletRequest request,
            @RequestParam Long applicationId) {
        String openId = (String) request.getAttribute("openid");
        return organizationService.revokeCreateOrganizationApplication(openId, applicationId);
    }
}