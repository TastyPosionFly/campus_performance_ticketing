package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.ApplicationRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationAuditCommand;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationPublicDto;
import org.example.campus_performance_ticketing.logic.dto.application.PendingApplicationDto;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
import org.example.campus_performance_ticketing.model.Application;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
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

@Service
@Validated
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final ApplicationTxService applicationTxService;
    private final JsonHelper jsonHelper;

    @Value("${file.base.url}")
    private String baseUrl;

    private final static Logger logger = Logger.getLogger(ApplicationService.class.getName());

    /**
     * 查询待处理申请列表
     * 管理员：看所有 (包括 PERFORMANCE_APPLY, CREATE_ORG 等)
     * 社长：只看 JOIN_ORG
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
                // === 管理员逻辑 ===
                // 管理员可以看到：创建社团、解散社团、**演出申请** 等
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
                // === 组织首领逻辑 ===
                // 只能看 JOIN_ORG，且只能看自己管理的社团

                // 1. 权限校验
                if (applicationType != null && !"JOIN_ORG".equals(applicationType)) {
                    return ApiResponse.fail("权限不足：组织负责人只能查看加入组织申请");
                }

                // 2. 找到“我作为 leader 的组织 ID 列表”
                List<OrganizationInfo> myLeaderOrgs = organizationInfoRepository.findAllByLeaderId(userInfo.getId());
                if (myLeaderOrgs == null || myLeaderOrgs.isEmpty()) {
                    return ApiResponse.success(List.of());
                }
                List<Long> myOrgIds = myLeaderOrgs.stream().map(OrganizationInfo::getId).toList();

                // 3. 查询
                if (status != null) {
                    applicationList = applicationRepository
                            .findByApplicationTypeAndTargetIdInAndStatus("JOIN_ORG", myOrgIds, status);
                } else {
                    applicationList = applicationRepository
                            .findByApplicationTypeAndTargetIdIn("JOIN_ORG", myOrgIds);
                }
            }

            // === DTO 转换 ===
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

                // 辅助方法解析 display info (例如从 extraData 解析组织名，或者如果是演出申请，这里可能解析不出演出名)
                // 建议：对于 PERFORMANCE_APPLY，如果 extraData 里存了 title，这里就能显示。
                jsonHelper.parseDisplayDtoFields(app, dto, objectMapper);

                resultList.add(dto);
            }

            return ApiResponse.success(resultList);

        } catch (Exception e) {
            logger.log(Level.WARNING, "申请查询失败", e);
            return ApiResponse.fail("申请查询失败: " + e.getMessage());
        }
    }

    /**
     * 批量审核：允许部分失败
     */
    public ApiResponse<Void> batchChangeApplicationStatus(@NotBlank String openId,
                                                          @NotEmpty @Valid List<ApplicationAuditCommand> audits) {

        UserInfo operator = userRepository.findByOpenid(openId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        int successCount = 0;
        int failCount = 0;
        StringBuilder failMsgs = new StringBuilder();

        for (ApplicationAuditCommand cmd : audits) {
            // 基础参数校验
            if (cmd == null || cmd.getApplicationId() == null || cmd.getNewStatus() == null
                    || !(cmd.getNewStatus() == 2 || cmd.getNewStatus() == 3) // 2通过 3拒绝
                    || !StringUtils.hasText(cmd.getReason())) {
                failCount++;
                failMsgs.append("参数异常;");
                continue;
            }

            try {
                // 调用独立事务处理单条
                applicationTxService.processOneAudit(operator.getId(), cmd);
                successCount++;
            } catch (Exception e) {
                failCount++;
                logger.log(Level.SEVERE, "���理申请失败, applicationId=" + cmd.getApplicationId(), e);
                failMsgs.append(String.format("申请%d:%s;", cmd.getApplicationId(), e.getMessage()));
            }
        }

        String msg = String.format("审核成功%d条，失败%d条。%s", successCount, failCount, failMsgs);

        ApiResponse <Void> response = ApiResponse.success(null);
        response.setMessage(msg);

        return response;
    }

    /**
     * 查询个人申请记录（仅返回公开信息）
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<ApplicationPublicDto>> getUserApplications(@NotBlank String openId,
                                                                       String applicationType) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            List<Application> applications;
            if (applicationType != null) {
                applications = applicationRepository
                        .findByApplicantIdAndApplicationTypeOrderByApplyTimeDesc(user.getId(), applicationType);
            } else {
                applications = applicationRepository.findByApplicantIdOrderByApplyTimeDesc(user.getId());
            }

            List<ApplicationPublicDto> dtoList = applications.stream().map(app -> {
                ApplicationPublicDto dto = new ApplicationPublicDto();
                dto.setId(app.getId());

                // User Info
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
                return dto;
            }).toList();

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

            ApiResponse <Void> response = ApiResponse.success(null);
            response.setMessage("申请已撤销");

            return response;
        } catch (Exception e) {
            logger.warning("撤销申请失败: " + e.getMessage());
            return ApiResponse.fail("撤销申请失败: " + e.getMessage());
        }
    }
}