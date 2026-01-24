package org.example.campus_performance_ticketing.logic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.ApplicationRepository;
import org.example.campus_performance_ticketing.dao.OrganizationAlbumRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.organization.PublicOrganizationInfo;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
import org.example.campus_performance_ticketing.model.Application;
import org.example.campus_performance_ticketing.model.OrganizationAlbum;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 组织服务类
 */
@Service
@Validated
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationInfoRepository organizationInfoRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final OrganizationAlbumRepository organizationAlbumRepository;

    private static final Logger logger = Logger.getLogger(OrganizationService.class.getName());

    @Value ("${file.base.url}")
    private String baseUrl;


    /**
     * 申请创建组织
     */
    @Transactional
    public ApiResponse<Void> applyCreateOrganization(@NotBlank String openId,
                                                     @NotBlank String orgName,
                                                     @NotBlank String orgDescription,
                                                     @NotBlank String avatarUrl) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            // 构造 extraData JSON（推荐用fastjson、Jackson或手工拼接）
            String extraData = String.format("{\"orgName\":\"%s\",\"orgDescription\":\"%s\",\"avatarUrl\":\"%s\"}",
                    orgName.replace("\"", "\\\""),
                    orgDescription == null ? "" : orgDescription.replace("\"", "\\\""),
                    avatarUrl == null ? "" : avatarUrl.replace("\"", "\\\"")
            );

            Application application = new Application();
            application.setApplicant(user); // 如果你是Long类型用setApplicantId(user.getId());
            application.setApplicationType("CREATE_ORG");
            application.setExtraData(extraData);
            application.setStatus(1); // 1-待审核

            applicationRepository.save(application);

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("组织创建申请已提交，等待审核");
            return resp;
        } catch (Exception e) {
            logger.warning("申请创建组织失败: " + e.getMessage());
            return ApiResponse.fail("申请创建组织失败: " + e.getMessage());
        }
    }

    /**
     * 更换组织首领
     */
    @Transactional
    public ApiResponse<Void> changeOrganizationLeader(@NotBlank String openId,
                                                      @NotNull Long orgId,
                                                      @NotNull Long newLeaderId) {

        try {
            UserInfo oldLeader = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationInfo organization = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            if (oldLeader != organization.getLeader() && !"ADMIN".equals(oldLeader.getRole()) && !"SUPER_ADMIN".equals(oldLeader.getRole())) {
                return ApiResponse.fail("只有当前首领或管理员才能更换组织首领");
            }

            UserInfo newLeader = userRepository.findById(newLeaderId)
                    .orElseThrow(() -> new IllegalArgumentException("新首领用户不存在"));

            // 更新组织信息中的首领字段
            organization.setLeader(newLeader);
            organizationInfoRepository.save(organization);

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("组织首领更换成功");
            return resp;
        } catch (Exception e) {
            logger.warning("更换组织首领失败: " + e.getMessage());
            return ApiResponse.fail("更换组织首领失败: " + e.getMessage());
        }
    }

    /**
     * 查看所有组织
     */
    public ApiResponse<List<PublicOrganizationInfo>> getAllOrganizations() {
        try {
            Iterable<OrganizationInfo> organizations = organizationInfoRepository.findAll();

            // 转换为 PublicOrganizationInfo 列表
            List<PublicOrganizationInfo> orgDtos = new ArrayList<>();
            for (OrganizationInfo org : organizations) {
                if (org.getStatus() == 2 || org.getStatus() == 3) {
                    continue; // 跳过待审核和已解散的组织
                }

                // 构造组织首领的 PublicUserInfo
                PublicUserInfo leader = new PublicUserInfo();
                leader.setNickname(org.getLeader().getNickname());
                leader.setAvatar(AvatarUrlUtil.buildAvatarUrl(org.getLeader().getAvatar(), baseUrl));
                leader.setMajor(org.getLeader().getMajor());
                leader.setCollege(org.getLeader().getCollege());
                leader.setStatus(org.getLeader().getStatus());

                // 构造 PublicOrganizationInfo
                PublicOrganizationInfo publicOrganizationInfo = new PublicOrganizationInfo();
                publicOrganizationInfo.setId(org.getId());
                publicOrganizationInfo.setName(org.getName());
                publicOrganizationInfo.setDescription(org.getDescription());
                publicOrganizationInfo.setAvatarUrl(AvatarUrlUtil.buildAvatarUrl(org.getAvatarUrl(), baseUrl));
                publicOrganizationInfo.setLeader(leader);

                orgDtos.add(publicOrganizationInfo);
            }

            ApiResponse<List<PublicOrganizationInfo>> resp = ApiResponse.success(orgDtos);
            resp.setMessage("组织列表获取成功");
            return resp;
        } catch (Exception e) {
            logger.warning("获取组织列表失败: " + e.getMessage());
            return ApiResponse.fail("获取组织列表失败: " + e.getMessage());
        }
    }


    /**
     * 查看单个组织详情
     */
    public ApiResponse<PublicOrganizationInfo> getOrganizationById(@NotNull Long orgId) {

        try {
            OrganizationInfo org = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            if (org.getStatus() == 2 || org.getStatus() == 3) {
                return ApiResponse.fail("组织不可用");
            }

            // 构造组织首领的 PublicUserInfo
            PublicUserInfo leader = new PublicUserInfo();
            leader.setNickname(org.getLeader().getNickname());
            leader.setAvatar(AvatarUrlUtil.buildAvatarUrl(org.getLeader().getAvatar(), baseUrl));
            leader.setMajor(org.getLeader().getMajor());
            leader.setCollege(org.getLeader().getCollege());
            leader.setStatus(org.getLeader().getStatus());

            // 构造 PublicOrganizationInfo
            PublicOrganizationInfo publicOrganizationInfo = new PublicOrganizationInfo();
            publicOrganizationInfo.setId(org.getId());
            publicOrganizationInfo.setName(org.getName());
            publicOrganizationInfo.setDescription(org.getDescription());
            publicOrganizationInfo.setAvatarUrl(AvatarUrlUtil.buildAvatarUrl(org.getAvatarUrl(), baseUrl));
            publicOrganizationInfo.setLeader(leader);

            ApiResponse<PublicOrganizationInfo> resp = ApiResponse.success(publicOrganizationInfo);
            resp.setMessage("组织详情获取成功");
            return resp;
        } catch (Exception e) {
            logger.warning("获取组织详情失败: " + e.getMessage());
            return ApiResponse.fail("获取组织详情失败: " + e.getMessage());
        }
    }

    /**
     * 解散组织
     */
    @Transactional
    public ApiResponse<Void> disbandOrganizationApply(@NotBlank String openId,
                                                      @NotNull Long orgId) {

        if (!StringUtils.hasText(openId) || orgId == null) {
            return ApiResponse.fail("参数不能为空");
        }

        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationInfo organization = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            if (!user.equals(organization.getLeader()) && !"ADMIN".equals(user.getRole()) && !"SUPER_ADMIN".equals(user.getRole())) {
                return ApiResponse.fail("只有组织首领或管理员才能解散组织");
            }

            // 提交审核
            Application app = new Application();
            app.setApplicant(user); // 如果你是Long类型用setApplicantId(user.getId());
            app.setApplicationType("DISBAND_ORG");
            app.setTargetId(organization.getId());
            app.setExtraData("{\"orgId\":" + orgId + "}");
            app.setStatus(1); // 1-待审核

            applicationRepository.save(app);

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("组织解散申请已提交，等待审核");
            return resp;
        } catch (Exception e) {
            logger.warning("解散组织申请失败: " + e.getMessage());
            return ApiResponse.fail("解散组织申请失败: " + e.getMessage());
        }
    }

    /**
     * 定期物理删除已解散组织（status = 3）
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteInactiveOrganizationsAndImagesPeriodically() {
        try {
            List<OrganizationInfo> disbandedOrgs = organizationInfoRepository.findByStatus(3);
            List<Long> orgIds = disbandedOrgs.stream().map(OrganizationInfo::getId).toList();

            // 删除图片文件（如有需要）
            List<OrganizationAlbum> albums = organizationAlbumRepository.findByOrganizationIdIn(orgIds);
            for (OrganizationAlbum album : albums) {
                FileUtil.deletePhysicalFile(album.getPhotoUrl());
            }

            // 删除组织头像文件
            for (OrganizationInfo org : disbandedOrgs) {
                FileUtil.deletePhysicalFile(org.getAvatarUrl());
            }

            // 删除图片数据库记录
            organizationAlbumRepository.deleteByOrganizationIdIn(orgIds);

            // 删除组织本身
            organizationInfoRepository.deleteByStatus(3);

            logger.info("定时清理已解散组织及图片成功！");
        } catch (Exception e) {
            logger.severe("定时清理组织及图片失败: " + e.getMessage());
        }
    }

}
