package org.example.campus_performance_ticketing.api;

import jakarta.servlet.http.HttpServletRequest;
import org.example.campus_performance_ticketing.logic.OrganizationAlbumService;
import org.example.campus_performance_ticketing.logic.OrganizationService;
import org.example.campus_performance_ticketing.logic.OrganizationMemberService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.organization.*;
import org.example.campus_performance_ticketing.model.OrganizationAlbum;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/organization")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationMemberService organizationMemberService;
    private final OrganizationAlbumService organizationAlbumService;

    @Value("${org.avatar.upload-dir}")
    private String avatarUploadDir;

    @Value("${org.album.upload-dir}")
    private String albumUploadDir;

    @Autowired
    public OrganizationController(OrganizationService organizationService,
                                  OrganizationMemberService organizationMemberService,
                                  OrganizationAlbumService organizationAlbumService) {
        this.organizationAlbumService = organizationAlbumService;
        this.organizationService = organizationService;
        this.organizationMemberService = organizationMemberService;
    }

    /**
     * 申请创建组织
     */
    @PostMapping("/apply")
    public ApiResponse<Void> applyCreateOrganization(
            HttpServletRequest request,
            @RequestPart("body") ApplyOrganizationRequest body,
            @RequestPart(name = "avatarFile", required = false) MultipartFile avatarFile
    ) {
        String openId = (String) request.getAttribute("openid");
        String avatarPath = null;
        String safeDir = FileUtil.normalizeUploadDir(avatarUploadDir);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                avatarPath = FileUtil.saveAvatar(avatarFile, safeDir, null);
            } catch (IOException e) {
                return ApiResponse.fail("头像上传失败：" + e.getMessage());
            }
        } else if (body.getAvatarUrl() != null && body.getAvatarUrl().startsWith("http")) {
            try {
                avatarPath = FileUtil.saveAvatarFromUrl(body.getAvatarUrl(), safeDir, null);
            } catch (IOException e) {
                return ApiResponse.fail("头像处理失败：" + e.getMessage());
            }
        }
        return organizationService.applyCreateOrganization(
                openId,
                body.getOrgName(),
                body.getOrgDescription(),
                avatarPath
        );
    }

    /**
     * 更换组织首领
     */
    @PostMapping("/change-leader")
    public ApiResponse<Void> changeOrganizationLeader(
            HttpServletRequest request,
            @RequestParam Long orgId,
            @RequestParam Long newLeaderId
    ) {
        String openId = (String) request.getAttribute("openid");
        return organizationService.changeOrganizationLeader(openId, orgId, newLeaderId);
    }

    /**
     * 获取所有组织（不包含待审核/已解散）
     */
    @GetMapping("/all")
    public ApiResponse<List<PublicOrganizationInfo>> getAllOrganizations() {
        return organizationService.getAllOrganizations();
    }

    /**
     * 获取指定组织详情
     */
    @GetMapping("/{orgId}")
    public ApiResponse<PublicOrganizationInfo> getOrganizationById(@PathVariable Long orgId) {
        return organizationService.getOrganizationById(orgId);
    }

    /**
     * 提交组织解散申请
     */
    @PostMapping("/disband")
    public ApiResponse<Void> disbandOrganization(
            HttpServletRequest request,
            @RequestParam Long orgId
    ) {
        String openId = (String) request.getAttribute("openid");
        return organizationService.disbandOrganizationApply(openId, orgId);
    }

    /**
     * 获取某个组织的成员列表（仅公开信息）
     */
    @GetMapping("/{orgId}/members")
    public ApiResponse<List<OrganizationMemberPublicDto>> listOrganizationMembers(@PathVariable Long orgId) {
        return organizationMemberService.listOrganizationMembers(orgId);
    }

    /**
     * 获取自己加入的组织列表
     */
    @GetMapping("/my-organizations")
    public ApiResponse<List<UserOrganizationMemberDto>> listUserOrganizations(HttpServletRequest request) {
        String openId = (String) request.getAttribute("openid");
        return organizationMemberService.listUserOrganizations(openId);
    }

    // ================= 组织成员相关接口 ==================

    /**
     * 申请加入组织（用DTO）
     * body: { "orgId":123, "reason":"想加入" }
     */
    @PostMapping("/member/apply")
    public ApiResponse<Void> applyJoinOrganization(
            HttpServletRequest request,
            @RequestBody ApplyJoinOrganizationRequest body
    ) {
        String openId = (String) request.getAttribute("openid");
        return organizationMemberService.applyJoinOrganization(openId, body.getOrgId(), body.getReason());
    }

    /**
     * 更改组织成员身份（不能直接设为LEADER, 只能管理员操作）
     */
    @PostMapping("/member/change-role")
    public ApiResponse<Void> changeOrganizationMemberRole(
            HttpServletRequest request,
            @RequestBody ChangeMemberRoleRequest body
    ) {
        String openId = (String) request.getAttribute("openid");
        Long orgId = body.getOrgId();
        Long memberId = body.getMemberId();
        String newRole = body.getNewRole();
        return organizationMemberService.changeOrganizationMemberRole(openId, orgId, memberId, newRole);
    }

    /**
     * 退出组织
     */
    @PostMapping("/member/quit")
    public ApiResponse<Void> quitOrganization(
            HttpServletRequest request,
            @RequestParam Long orgId
    ) {
        String openId = (String) request.getAttribute("openid");
        return organizationMemberService.quitOrganization(openId, orgId);
    }

    /**
     * 踢出组织成员（管理员操作）
     */
    @PostMapping("/member/kick")
    public ApiResponse<Void> kickOutOrganizationMember(
            HttpServletRequest request,
            @RequestParam Long orgId,
            @RequestParam Long memberId
    ) {
        String openId = (String) request.getAttribute("openid");
        return organizationMemberService.kickOutOrganizationMember(openId, orgId, memberId);
    }



    // ================= 组织相册相关接口 ==================

    /**
     * 上传组织相册图片
     * 前端 multipart/form-data，body部分为JSON，photoFile为文件
     * - 方案1: 传 organizationId + photoUrl (仅URL上传)
     * - 方案2: 传 organizationId + photoFile (仅文件上传)
     * - organizationId 必须
     */
    @PostMapping("/album/upload")
    public ApiResponse<Void> uploadAlbumPhoto(
            HttpServletRequest request,
            @RequestPart("body") UploadAlbumPhotoRequest body,
            @RequestPart(name = "photoFile", required = false) MultipartFile photoFile
    ) {
        String openId = (String) request.getAttribute("openid");
        return organizationAlbumService.uploadAlbumPhoto(openId, body, photoFile);
    }

    /**
     * 删除组织相册图片
     */
    @PostMapping("/album/delete")
    public ApiResponse<Void> deleteAlbumPhoto(
            HttpServletRequest request,
            @RequestParam Long photoId
    ) {
        String openId = (String) request.getAttribute("openid");
        return organizationAlbumService.deletePhoto(openId, photoId);
    }

    /**
     * 查询组织相册图片列表
     */
    @PostMapping("/album/list")
    public ApiResponse<List<PublicOrganizationAlbumInfo>> getOrganizationAlbumPhotos(
            @RequestParam Long organizationId
    ) {
        return organizationAlbumService.getOrganizationPhotos(organizationId);
    }
}