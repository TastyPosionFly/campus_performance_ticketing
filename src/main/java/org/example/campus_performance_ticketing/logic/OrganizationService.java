package org.example.campus_performance_ticketing.logic;

import org.example.campus_performance_ticketing.dao.OrganizationRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.Organization;
import org.example.campus_performance_ticketing.util.JwtTokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final JwtTokenUtil jwtTokenUtil;

    public OrganizationService(OrganizationRepository organizationRepository,
                               JwtTokenUtil jwtTokenUtil) {
        this.organizationRepository = organizationRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 用户申请注册组织
     */
    @Transactional
    public ApiResponse<Organization> applyOrganization(Long userId, String name, String description) {
        try {
            Organization org = new Organization();
            org.setName(name);
            org.setDescription(description);
            org.setLeaderUserId(userId);
            org.setStatus(0); // 待审核

            Organization saved = organizationRepository.save(org);
            return ApiResponse.success(saved);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 审核组织申请
     */
    @Transactional
    public ApiResponse<Organization> approveOrganization(String token, Long organizationId, boolean approve) {
        try {
            String role = (String) jwtTokenUtil.parseToken(token).get("role");

            if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
                return ApiResponse.fail("没有权限审核组织");
            }

            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            if (approve) {
                // 审核通过
                org.setStatus(1); // 1=正常
                Organization updated = organizationRepository.save(org);
                return ApiResponse.success(updated);
            } else {
                // 审核拒绝，直接删除
                organizationRepository.delete(org);
                return ApiResponse.fail("组织审核被拒绝并已删除");
            }

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 封禁 / 解封组织
     * @param adminToken 管理员 JWT
     * @param organizationId 待操作组织ID
     * @param ban true=封禁, false=解封
     */
    @Transactional
    public ApiResponse<Organization> banOrUnbanOrganization(String adminToken, Long organizationId, boolean ban) {
        try {
            String role = (String) jwtTokenUtil.parseToken(adminToken).get("role");

            if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
                return ApiResponse.fail("没有权限操作组织封禁");
            }

            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            org.setStatus(ban ? 2 : 1); // 2=封禁, 1=正常
            Organization updated = organizationRepository.save(org);
            return ApiResponse.success(updated);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
