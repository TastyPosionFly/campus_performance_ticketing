package org.example.campus_performance_ticketing.logic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.*;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.organization.PublicOrganizationInfo;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
import org.example.campus_performance_ticketing.model.*;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.Objects;

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
    private final OrganizationMemberRepository organizationMemberRepository;

    private static final Logger logger = Logger.getLogger(OrganizationService.class.getName());

    @Value ("${file.base.url}")
    private String baseUrl;

    @Value("${org.avatar.upload-dir}")
    private String orgAvatarDir;

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

    @Transactional
    public ApiResponse<Void> updateOrganizationInfo(@NotBlank String openId,
                                                    @NotNull Long orgId,
                                                    String name,
                                                    String description,
                                                    MultipartFile avatarFile) {
        try {
            // 操作者校验
            UserInfo operator = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationInfo organization = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            Long currentLeaderId = organization.getLeader() == null ? null : organization.getLeader().getId();
            boolean operatorIsLeader = Objects.equals(currentLeaderId, operator.getId());
            boolean operatorIsAdmin = "ADMIN".equals(operator.getRole()) || "SUPER_ADMIN".equals(operator.getRole());
            String oldAvatarPath = organization.getAvatarUrl(); // 先记录旧头像路径

            if (!operatorIsLeader && !operatorIsAdmin) {
                return ApiResponse.fail("权限不足：只有组织首领或管理员可以修改组织信息");
            }

            boolean changed = false;
            String newAvatarPath = null; // track new file in case we need to cleanup on failure

            // 更新名称（若传入且不同）
            if (StringUtils.hasText(name) && !name.equals(organization.getName())) {
                // 可选：名称唯一性校验
                organization.setName(name);
                changed = true;
            }

            // 更新简介（可能为空字符串也视为更新）
            if (description != null && !description.equals(organization.getDescription())) {
                organization.setDescription(description);
                changed = true;
            }

            // 处理头像文件上传：使用 FileUtil 保存并获取保存路径
            if (avatarFile != null && !avatarFile.isEmpty()) {
                if (orgAvatarDir == null || orgAvatarDir.isBlank()) {
                    return ApiResponse.fail("服务器未配置组织头像上传目录，无法保存文件");
                }

                try {
                    String normalizedDir = FileUtil.normalizeUploadDir(orgAvatarDir);
                    newAvatarPath = FileUtil.saveAvatar(avatarFile, normalizedDir, null); // 返回的是 final path (dir + filename)
                    if (newAvatarPath == null) {
                        return ApiResponse.fail("保存头像失败");
                    }
                    // 设到组织实体上（暂未删除旧文件，等 DB 保存成功后再删除；如果删除失败记录日志）
                    organization.setAvatarUrl(newAvatarPath);
                    changed = true;
                } catch (IOException e) {
                    logger.warning("保存组织头像失败: " + e.getMessage());
                    return ApiResponse.fail("保存组织头像失败: " + e.getMessage());
                }
            }

            if (changed) {
                try {
                    organizationInfoRepository.save(organization);
                    // DB 保存成功后再删除旧头像文件（如果有且新旧路径不同）
                    if (oldAvatarPath != null && !oldAvatarPath.isBlank() && !oldAvatarPath.equals(newAvatarPath)) {
                        FileUtil.deletePhysicalFile(oldAvatarPath);
                    }
                } catch (Exception e) {
                    // 如果 DB 保存出错，尝试删除刚刚上传的新文件以避免垃圾文件
                    if (newAvatarPath != null) {
                        try {
                            FileUtil.deletePhysicalFile(newAvatarPath);
                        } catch (Exception ex) {
                            logger.warning("回滚时删除新头像文件失败: " + ex.getMessage());
                        }
                    }
                    throw e; // 继续上抛到外层统一处理
                }
            }

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("组织信息更新成功");
            return resp;
        } catch (Exception e) {
            logger.warning("更新组织信息失败: " + e.getMessage());
            return ApiResponse.fail("更新组织信息失败: " + e.getMessage());
        }
    }

    /**
     * 更换组织首领
     */
    public ApiResponse<Void> changeOrganizationLeader(@NotBlank String openId,
                                                      @NotNull Long orgId,
                                                      @NotNull Long newLeaderId) {

        try {
            // operator = 发起更换操作的用户（可能是当前首领，也可能是 ADMIN/SUPER_ADMIN）
            UserInfo operator = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationInfo organization = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            Long currentLeaderId = organization.getLeader() == null ? null : organization.getLeader().getId();

            // 权限校验：必须是当前首领或管理员/超级管理员
            boolean operatorIsLeader = Objects.equals(currentLeaderId, operator.getId());
            boolean operatorIsAdmin = "ADMIN".equals(operator.getRole()) || "SUPER_ADMIN".equals(operator.getRole());
            if (!operatorIsLeader && !operatorIsAdmin) {
                return ApiResponse.fail("只有当前首领或管理员才能更换组织首领");
            }

            UserInfo newLeader = userRepository.findById(newLeaderId)
                    .orElseThrow(() -> new IllegalArgumentException("新首领用户不存在"));

            // 如果新首领就是当前首领，直接返回成功（或提示）
            if (currentLeaderId != null && currentLeaderId.equals(newLeader.getId())) {
                ApiResponse<Void> resp = ApiResponse.success(null);
                resp.setMessage("新首领与当前首领相同，无需更换");
                return resp;
            }

            // ====== 同步更新 OrganizationMember 表中的信息 ======
            try {
                // 如果存在原首领，先把原首领的成员角色调整为 MEMBER（避免覆盖其他管理员设置）
                if (currentLeaderId != null) {
                    Optional<OrganizationMember> oldMemberOpt = organizationMemberRepository
                            .findByOrganizationIdAndUserId(organization.getId(), currentLeaderId);
                    logger.info("原首领 id=" + currentLeaderId + " 的成员记录存在: " + oldMemberOpt.isPresent());
                    if (oldMemberOpt.isPresent()) {
                        OrganizationMember oldMember = oldMemberOpt.get();
                        if ("LEADER".equals(oldMember.getMemberRole())) {
                            oldMember.setMemberRole("MEMBER");
                            organizationMemberRepository.save(oldMember);
                        }
                    } else {
                        // 如果没有成员记录，不强制创建（业务可选），这里只记录日志
                        logger.info("原首领 id=" + currentLeaderId + " 在 organization_member 中没有记录");
                    }

                    // 如果操作者是 ADMIN/SUPER_ADMIN（即非组织首领，由管理员替换首领），将原首领的全局权限降为 USER
                    if (operatorIsAdmin && !operatorIsLeader) {
                        try {
                            UserInfo originalLeader = userRepository.findById(currentLeaderId)
                                    .orElse(null);
                            if (originalLeader != null) {
                                originalLeader.setRole("USER");
                                userRepository.save(originalLeader);
                                logger.info("已将原首领(id=" + currentLeaderId + ") 的权限降为 USER（由管理员操作）");
                            } else {
                                logger.warning("找不到原首领的用户记录 id=" + currentLeaderId);
                            }
                        } catch (Exception e) {
                            // 将原首领权限降级失败，记录但不阻止主流程（视业务可改为抛出异常回滚）
                            logger.warning("将原首领权限降为 USER 时出错: " + e.getMessage());
                        }
                    }
                }

                // 将新首领的成员角色设置为 LEADER（如果存在则更新，否则创建一条记录）
                Optional<OrganizationMember> newMemberOpt = organizationMemberRepository
                        .findByOrganizationIdAndUserId(organization.getId(), newLeader.getId());
                logger.info("新首领 " + newLeader.getNickname() + " 的成员记录存在: " + newMemberOpt.isPresent());
                if (newMemberOpt.isPresent()) {
                    OrganizationMember newMember = newMemberOpt.get();
                    newMember.setMemberRole("LEADER");
                    newMember.setStatus(1); // 确保为在组织中的状态
                    organizationMemberRepository.save(newMember);
                } else {
                    OrganizationMember newMember = new OrganizationMember();
                    newMember.setOrganization(organization);
                    newMember.setUser(newLeader);
                    newMember.setMemberRole("LEADER");
                    newMember.setStatus(1); // 在组织中
                    organizationMemberRepository.save(newMember);
                }
            } catch (Exception e) {
                // 成员表更新失败不应影响已更新的组织首领字段，但应记录错误
                logger.warning("更新组织成员表以同步首领变更时出错: " + e.getMessage());
            }

            // 最后更新组织信息中的首领字段
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
        // 保留原有行为的兼容方法（非分页），调用分页方法并返回第一页全部内容
        ApiResponse<org.springframework.data.domain.Page<PublicOrganizationInfo>> pageResp = getOrganizationsPaginated(0, Integer.MAX_VALUE);
        if (!pageResp.isSuccess()) return ApiResponse.fail(pageResp.getMessage());
        org.springframework.data.domain.Page<PublicOrganizationInfo> page = pageResp.getData();
        return ApiResponse.success(page == null ? new ArrayList<>() : page.getContent());
    }

    /**
     * 分页获取组织列表（排除 status = 2 表示已解散/不可见）
     * @param page 0-based 页码
     * @param size 每页大小
     */
    public ApiResponse<org.springframework.data.domain.Page<PublicOrganizationInfo>> getOrganizationsPaginated(int page, int size) {
        try {
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.max(1, size));
            org.springframework.data.domain.Page<OrganizationInfo> pageResult = organizationInfoRepository.findByStatusNot(2, pageable);

            List<PublicOrganizationInfo> orgDtos = new ArrayList<>();
            for (OrganizationInfo org : pageResult.getContent()) {
                if (org == null) continue;

                PublicUserInfo leader = new PublicUserInfo();
                if (org.getLeader() != null) {
                    leader.setNickname(org.getLeader().getNickname());
                    leader.setAvatar(AvatarUrlUtil.buildAvatarUrl(org.getLeader().getAvatar(), baseUrl));
                    leader.setMajor(org.getLeader().getMajor());
                    leader.setCollege(org.getLeader().getCollege());
                    leader.setStatus(org.getLeader().getStatus());
                }

                PublicOrganizationInfo publicOrganizationInfo = new PublicOrganizationInfo();
                publicOrganizationInfo.setId(org.getId());
                publicOrganizationInfo.setName(org.getName());
                publicOrganizationInfo.setDescription(org.getDescription());
                publicOrganizationInfo.setStatus(org.getStatus());
                publicOrganizationInfo.setAvatarUrl(AvatarUrlUtil.buildAvatarUrl(org.getAvatarUrl(), baseUrl));
                publicOrganizationInfo.setLeader(leader);

                orgDtos.add(publicOrganizationInfo);
            }

            org.springframework.data.domain.Page<PublicOrganizationInfo> dtoPage = new org.springframework.data.domain.PageImpl<>(orgDtos, pageable, pageResult.getTotalElements());

            ApiResponse<org.springframework.data.domain.Page<PublicOrganizationInfo>> resp = ApiResponse.success(dtoPage);
            resp.setMessage("组织列表获取成功");
            return resp;
        } catch (Exception e) {
            logger.warning("分页获取组织列表失败: " + e.getMessage());
            return ApiResponse.fail("分页获取组织列表失败: " + e.getMessage());
        }
    }



    /**
     * 按组织名称模糊搜索
     */
    public ApiResponse<List<PublicOrganizationInfo>> searchOrganizationsByName(@NotBlank String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return ApiResponse.fail("搜索关键字不能为空");
        }

        try {
            String q = keyword.trim();

            // 在 repository 层执行模糊不区分大小写查询，同时排除 status = 2（已解散/不可见）
            List<OrganizationInfo> matchedOrgs = organizationInfoRepository
                    .findByNameContainingIgnoreCaseAndStatusNot(q, 2);

            List<PublicOrganizationInfo> results = new ArrayList<>();
            for (OrganizationInfo org : matchedOrgs) {
                if (org == null) continue;

                // 构造组织首领的 PublicUserInfo（注意 leader 可能为空，需要防御）
                PublicUserInfo leader = new PublicUserInfo();
                if (org.getLeader() != null) {
                    leader.setNickname(org.getLeader().getNickname());
                    leader.setAvatar(AvatarUrlUtil.buildAvatarUrl(org.getLeader().getAvatar(), baseUrl));
                    leader.setMajor(org.getLeader().getMajor());
                    leader.setCollege(org.getLeader().getCollege());
                    leader.setStatus(org.getLeader().getStatus());
                }

                PublicOrganizationInfo publicOrganizationInfo = new PublicOrganizationInfo();
                publicOrganizationInfo.setId(org.getId());
                publicOrganizationInfo.setName(org.getName());
                publicOrganizationInfo.setDescription(org.getDescription());
                publicOrganizationInfo.setStatus(org.getStatus());
                publicOrganizationInfo.setAvatarUrl(AvatarUrlUtil.buildAvatarUrl(org.getAvatarUrl(), baseUrl));
                publicOrganizationInfo.setLeader(leader);

                results.add(publicOrganizationInfo);
            }

            ApiResponse<List<PublicOrganizationInfo>> resp = ApiResponse.success(results);
            resp.setMessage("按名称搜索组织成功，匹配数量: " + results.size());
            return resp;
        } catch (Exception e) {
            logger.warning("按名称搜索组织失败: " + e.getMessage());
            return ApiResponse.fail("按名称搜索组织失败: " + e.getMessage());
        }
    }

    /**
     * 分页按名称模糊搜索组织（排除 status = 2） - 用于 controller 的分页搜索
     */
    public ApiResponse<Page<PublicOrganizationInfo>> searchOrganizationsByNamePaginated(@NotBlank String keyword, int page, int size) {
        if (!StringUtils.hasText(keyword)) {
            return ApiResponse.fail("搜索关键字不能为空");
        }

        try {
            String q = keyword.trim();
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.max(1, size));
            org.springframework.data.domain.Page<OrganizationInfo> pageResult = organizationInfoRepository.findByNameContainingIgnoreCaseAndStatusNot(q, 2, pageable);

            List<PublicOrganizationInfo> results = new ArrayList<>();
            for (OrganizationInfo org : pageResult.getContent()) {
                PublicUserInfo leader = new PublicUserInfo();
                if (org.getLeader() != null) {
                    leader.setNickname(org.getLeader().getNickname());
                    leader.setAvatar(AvatarUrlUtil.buildAvatarUrl(org.getLeader().getAvatar(), baseUrl));
                    leader.setMajor(org.getLeader().getMajor());
                    leader.setCollege(org.getLeader().getCollege());
                    leader.setStatus(org.getLeader().getStatus());
                }

                PublicOrganizationInfo publicOrganizationInfo = new PublicOrganizationInfo();
                publicOrganizationInfo.setId(org.getId());
                publicOrganizationInfo.setName(org.getName());
                publicOrganizationInfo.setDescription(org.getDescription());
                publicOrganizationInfo.setStatus(org.getStatus());
                publicOrganizationInfo.setAvatarUrl(AvatarUrlUtil.buildAvatarUrl(org.getAvatarUrl(), baseUrl));
                publicOrganizationInfo.setLeader(leader);

                results.add(publicOrganizationInfo);
            }

            Page<PublicOrganizationInfo> dtoPage = new PageImpl<>(results, pageable, pageResult.getTotalElements());
            ApiResponse<Page<PublicOrganizationInfo>> resp = ApiResponse.success(dtoPage);
            resp.setMessage("按名称搜索组织成功，匹配数量: " + dtoPage.getTotalElements());
            return resp;
        } catch (Exception e) {
            logger.warning("分页按名称搜索组织失败: " + e.getMessage());
            return ApiResponse.fail("分页按名称搜索组织失败: " + e.getMessage());
        }
    }

    /**
     * 查看单个组织详情
     */
    public ApiResponse<PublicOrganizationInfo> getOrganizationById(@NotNull Long orgId) {

        try {
            OrganizationInfo org = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            if (org.getStatus() == 2) {
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
            publicOrganizationInfo.setStatus(org.getStatus());
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

            // ====== 新增：防止重复提交相同组织的解散申请（在未审核通过之前只能提交一次） ======
            List<Application> pendingDisband = applicationRepository.findByApplicationTypeAndTargetIdInAndStatus(
                    "DISBAND_ORG",
                    List.of(organization.getId()),
                    1
            );
            if (pendingDisband != null && !pendingDisband.isEmpty()) {
                return ApiResponse.fail("该组织已有解散申请正在审批中，不能重复提交");
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
     * 定期物理删除已解散组织（status = 2）
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteInactiveOrganizationsAndImagesPeriodically() {
        try {
            List<OrganizationInfo> disbandedOrgs = organizationInfoRepository.findByStatus(2);
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
