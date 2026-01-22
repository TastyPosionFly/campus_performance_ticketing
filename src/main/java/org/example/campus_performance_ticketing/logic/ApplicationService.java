package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.example.campus_performance_ticketing.dao.ApplicationRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.OrganizationMemberRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.*;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationAuditCommand;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationPublicDto;
import org.example.campus_performance_ticketing.logic.dto.application.PendingApplicationDto;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
import org.example.campus_performance_ticketing.model.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.OrganizationMember;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.JsonHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 申请服务类
 */

@Service
@Validated
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final ApplicationTxService applicationTxService;

    private final JsonHelper jsonHelper;

    private final static Logger logger = Logger.getLogger(ApplicationService.class.getName());

    @Value("${file.base.url}")
    private String baseUrl;

    public ApplicationService (ApplicationRepository applicationRepository,
                               UserRepository userRepository,
                               OrganizationInfoRepository organizationInfoRepository,
                               OrganizationMemberRepository organizationMemberRepository,
                                 ApplicationTxService applicationTxService,
                               JsonHelper jsonHelper
                               ) {
        this.applicationRepository = applicationRepository;

        this.applicationTxService = applicationTxService;
        this.userRepository = userRepository;
        this.organizationInfoRepository = organizationInfoRepository;
        this.organizationMemberRepository = organizationMemberRepository;

        this.jsonHelper = jsonHelper;
    }


    /**
     * 查询待处理申请列表（管理员或组织首领）
     * @param openId
     * @param applicationType
     * @param status
     * @return
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<PendingApplicationDto>> listApplications(@NotBlank String openId,
                                                                     String applicationType,
                                                                     Integer status) {
        try {
            UserInfo userInfo = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            boolean isAdmin = "ADMIN".equals(userInfo.getRole()) || "SUPER_ADMIN".equals(userInfo.getRole());

            List<Application> applicationList;

            if (isAdmin) {
                // === 管理员：沿用原有逻辑 ===
                if (applicationType != null && status != null) {
                    applicationList = applicationRepository.findByApplicationTypeAndStatus(applicationType, status);
                } else if (applicationType != null) {
                    applicationList = applicationRepository.findByApplicationType(applicationType);
                } else if (status != null) {
                    applicationList = applicationRepository.findByStatus(status);
                } else {
                    applicationList = applicationRepository.findAll();
                }
            } else {
                // === 组织首领：只能看 JOIN_ORG，且只能看自己组织的 JOIN_ORG ===

                // 1) 如果前端传了 applicationType，必须是 JOIN_ORG
                if (applicationType != null && !"JOIN_ORG".equals(applicationType)) {
                    return ApiResponse.fail("权限不足：组织负责人只能查看加入组织申请");
                }

                // 2) 找到“我作为 leader 的组织列表”
                // 你需要一个方法：organizationInfoRepository.findAllByLeaderId(userInfo.getId())
                List<OrganizationInfo> myLeaderOrgs = organizationInfoRepository.findAllByLeaderId(userInfo.getId());
                if (myLeaderOrgs == null || myLeaderOrgs.isEmpty()) {
                    return ApiResponse.success(List.of()); // 不是任何组织的负责人，返回空列表
                }

                List<Long> myOrgIds = myLeaderOrgs.stream().map(OrganizationInfo::getId).toList();

                // 3) 只查 JOIN_ORG + targetId in (我的组织)
                // 你需要这些 repository 方法之一（任选其一实现）：
                // - findByApplicationTypeAndTargetIdIn(...)
                // - findByApplicationTypeAndTargetIdInAndStatus(...)
                if (status != null) {
                    applicationList = applicationRepository
                            .findByApplicationTypeAndTargetIdInAndStatus("JOIN_ORG", myOrgIds, status);
                } else {
                    applicationList = applicationRepository
                            .findByApplicationTypeAndTargetIdIn("JOIN_ORG", myOrgIds);
                }
            }

            List<PendingApplicationDto> resultList = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();

            for (Application app : applicationList) {
                PendingApplicationDto dto = new PendingApplicationDto();
                dto.setApplicationId(app.getId());
                dto.setApplicationType(app.getApplicationType());
                dto.setApplicantOpenId(app.getApplicant().getOpenid());
                dto.setApplicantName(app.getApplicant().getNickname());
                dto.setApplyTime(app.getApplyTime());
                dto.setStatus(app.getStatus());
                dto.setTargetId(app.getTargetId());
                dto.setExtraData(app.getExtraData());

                jsonHelper.parseDisplayDtoFields(app, dto, objectMapper);
                resultList.add(dto);
            }

            ApiResponse<List<PendingApplicationDto>> resp = ApiResponse.success(resultList);
            resp.setMessage("查询成功");
            return resp;

        } catch (Exception e) {
            logger.warning("申请查询失败: " + e.getMessage());
            return ApiResponse.fail("申请查询失败: " + e.getMessage());
        }
    }


    /**
     * 批量审核：允许部分失败
     * 关键：这里不要 @Transactional
     */
    public ApiResponse<Void> batchChangeApplicationStatus(@NotBlank String openId,
                                                          @NotEmpty @Valid List<ApplicationAuditCommand> audits) {

        UserInfo operator = userRepository.findByOpenid(openId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        int successCount = 0;
        int failCount = 0;
        StringBuilder failMsgs = new StringBuilder();

        for (ApplicationAuditCommand cmd : audits) {
            // 参数校验尽量在外层先挡掉，减少事务开销
            if (cmd == null || cmd.getApplicationId() == null
                    || cmd.getNewStatus() == null
                    || !(cmd.getNewStatus() == 2 || cmd.getNewStatus() == 3)
                    || cmd.getReason() == null || cmd.getReason().isBlank()) {
                failCount++;
                failMsgs.append("参数异常;");
                continue;
            }

            try {
                // 每条审核进入“新事务”
                applicationTxService.processOneAudit(operator.getId(), cmd);
                successCount++;
            } catch (Exception e) {
                failCount++;

                // 打印完整堆栈，便于你在日志里看到真实原因（root cause）
                logger.log(Level.SEVERE,
                        "处理申请失败, applicationId=" + cmd.getApplicationId(), e);

                failMsgs.append(String.format("申请%d: %s;",
                        cmd.getApplicationId(), rootMessage(e)));
            }
        }

        String msg = String.format("审核成功%d条，失败%d条。%s", successCount, failCount, failMsgs);
        ApiResponse<Void> resp = ApiResponse.success(null);
        resp.setMessage(msg);
        return resp;
    }

    /**
     * 根据申请类型处理通过后的具体业务逻辑
     * @param app 审核通过的申请对象
     * @param oldExtraData 申请扩展信息
     * @param failMsgs 用于追加失败原因
     * @return true处理成功，false失败。
     */
    private boolean handleApplicationPassBusiness(Application app, String oldExtraData, StringBuilder failMsgs) {
        String type = app.getApplicationType();
        if ("CREATE_ORG".equals(type)) {
            // 创建组织逻辑
            String orgName = "", orgDescription = "", avatarUrl = "";
            try {
                JsonNode node = new ObjectMapper().readTree(oldExtraData);
                orgName = node.path("orgName").asText();
                orgDescription = node.path("orgDescription").asText();
                avatarUrl = node.path("avatarUrl").asText();
            } catch (Exception e) {
                failMsgs.append(String.format("申请%d:extraData 解析失败;", app.getId()));
                return false;
            }
            OrganizationInfo organization = new OrganizationInfo();
            organization.setName(orgName);
            organization.setDescription(orgDescription);
            organization.setAvatarUrl(avatarUrl);
            organization.setLeader(app.getApplicant());
            organization.setStatus(1);

            try {
                OrganizationInfo savedOrg = organizationInfoRepository.save(organization);

                OrganizationMember member = new OrganizationMember();
                member.setOrganization(savedOrg);
                member.setUser(app.getApplicant());
                member.setMemberRole("LEADER"); // "LEADER"
                member.setStatus(1); // 1-正常/激活

                organizationMemberRepository.save(member);

                app.setTargetId(savedOrg.getId());
                return true;
            } catch (Exception e) {
                failMsgs.append(String.format("申请%d:组织保存失败;%s", app.getId(), e.getMessage()));
                return false;
            }
        }

        else if ("JOIN_ORG".equals(type)) {
            // 加入组织逻辑
            OrganizationInfo organization = organizationInfoRepository.findById(app.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("目标组织不存在"));

            OrganizationMember member = new OrganizationMember();
            member.setOrganization(organization);
            member.setUser(app.getApplicant());
            member.setMemberRole("MEMBER"); // "MEMBER"
            member.setStatus(1); // 1-正常/激活

            try {
                organizationMemberRepository.save(member);
                return true;
            } catch (Exception e) {
                failMsgs.append(String.format("申请%d:加入组织保存失败;%s", app.getId(), e.getMessage()));
                return false;
            }
        }

        else if ("DISBAND_ORG".equals(type)){
            OrganizationInfo organizationInfo = organizationInfoRepository.findById(app.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("目标组织不存在"));

            organizationInfo.setStatus(3);

            organizationInfoRepository.save(organizationInfo); // 失败就抛异常
            return true;
        }

        // 可扩展其它类型：如申请加入组织、申请编辑相册等
        // else if ("JOIN_ORG".equals(type)) { ... }

        // 未处理类型
        return true;
    }

    /**
     * 查询个人申请记录（仅返回公开信息）
     */
    public ApiResponse<List<ApplicationPublicDto>> getUserApplications(@NotBlank String openId,
                                                                       String applicationType) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            List<Application> applications;

            if (applicationType != null){
                applications = applicationRepository
                        .findByApplicantIdAndApplicationTypeOrderByApplyTimeDesc(user.getId(), applicationType);
            } else {
                // 查询所有类型
                applications = applicationRepository.findByApplicantIdOrderByApplyTimeDesc(user.getId());
            }

            List<ApplicationPublicDto> dtoList = new ArrayList<>();
            for (Application app : applications) {
                ApplicationPublicDto dto = new ApplicationPublicDto();
                dto.setId(app.getId());

                // 只赋值公开信息
                UserInfo applicant = app.getApplicant();
                PublicUserInfo publicUser = new PublicUserInfo(
                        applicant.getNickname(),
                        AvatarUrlUtil.buildAvatarUrl(applicant.getAvatar(), baseUrl),
                        applicant.getMajor(),
                        applicant.getCollege(),
                        applicant.getStatus()
                );
                dto.setApplicant(publicUser);

                dto.setApplicationType(app.getApplicationType());
                dto.setTargetId(app.getTargetId());
                dto.setExtraData(app.getExtraData());
                dto.setStatus(app.getStatus());
                dto.setApplyTime(app.getApplyTime());
                dto.setApproveTime(app.getApproveTime());

                dtoList.add(dto);
            }
            return ApiResponse.success(dtoList);
        } catch (Exception e) {
            logger.warning("查询个人申请记录失败: " + e.getMessage());
            return ApiResponse.fail("查询个人申请记录失败: " + e.getMessage());
        }
    }

    /**
     * 撤销个人申请
     */
    @Transactional
    public ApiResponse<Void> revokeCreateApplication(@NotBlank String openId,
                                                     @NotNull Long applicationId) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }

        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("申请不存在"));

            if (!application.getApplicant().getId().equals(user.getId())) {
                throw new IllegalArgumentException("只能撤销自己的申请");
            }

            if (application.getStatus() != 1) {
                throw new IllegalArgumentException("只能撤销待审核的申请");
            }

            application.setStatus(4); // 4-撤销
            applicationRepository.save(application);

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("成功撤销申请");
            return resp;
        } catch (Exception e) {
            logger.warning("撤销申请失败: " + e.getMessage());
            return ApiResponse.fail("撤销申请失败: " + e.getMessage());
        }
    }

    private String rootMessage(Throwable e) {
        Throwable cur = e;
        while (cur.getCause() != null) cur = cur.getCause();
        String m = cur.getMessage();
        return cur.getClass().getSimpleName() + (m == null ? "" : (": " + m));
    }
}
