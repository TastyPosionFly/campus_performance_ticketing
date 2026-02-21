package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.ApplicationRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.PerformanceRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationAuditCommand;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationPublicDto;
import org.example.campus_performance_ticketing.logic.dto.application.PendingApplicationDto;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
import org.example.campus_performance_ticketing.model.Application;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.Performance;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.*;
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
    private final PerformanceRepository performanceRepository;

    @Value("${file.base.url}")
    private String baseUrl;

    private final static Logger logger = Logger.getLogger(ApplicationService.class.getName());

    /**
     * 查询待处理申请列表
     * 管理员：看所有 (包括 PERFORMANCE_APPLY, CREATE_ORG 等)
     * 社长：只看 JOIN_ORG
     */
    @Transactional(readOnly = true)
    public ApiResponse<Page<PendingApplicationDto>> listApplications(@NotBlank String openId,
                                                                     String applicationType,
                                                                     Integer status,
                                                                     int page,
                                                                     int size) {
        try {
            UserInfo userInfo = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            List<OrganizationInfo> organizationInfos = organizationInfoRepository.findAllByLeaderId(userInfo.getId());

            boolean isAdmin = "ADMIN".equals(userInfo.getRole()) || "SUPER_ADMIN".equals(userInfo.getRole());
            boolean isOrgLeader = organizationInfos != null && !organizationInfos.isEmpty();

            if (!isAdmin && !isOrgLeader) {
                return ApiResponse.fail("权限不足：仅管理员或组织负责人可查看待处理申请");
            }

            Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(
                            Math.max(0, page),
                            Math.max(1, size),
                            org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "applyTime")
                    );
            Page<Application> pageResult;

            if (isAdmin) {
                // 管理员：按 applicationType / status / both / all 支持分页
                if (applicationType != null && status != null) {
                    pageResult = applicationRepository.findByApplicationTypeAndStatus(applicationType, status, pageable);
                } else if (applicationType != null) {
                    pageResult = applicationRepository.findByApplicationType(applicationType, pageable);
                } else if (status != null) {
                    pageResult = applicationRepository.findByStatus(status, pageable);
                } else {
                    // fallback to all ordered by applyTime desc via repository (no direct pageable method existed)
                    pageResult = applicationRepository.findAll(pageable);
                }
            } else {
                // 组织首领：只能查看 JOIN_ORG 相关的申请，且只查看其管理的组织
                if (applicationType != null && !"JOIN_ORG".equals(applicationType)) {
                    return ApiResponse.fail("权限不足：组织负责人只能查看加入组织申请");
                }
                List<Long> myOrgIds = organizationInfos.stream().map(OrganizationInfo::getId).toList();

                if (status != null) {
                    pageResult = applicationRepository.findByApplicationTypeAndTargetIdInAndStatus("JOIN_ORG", myOrgIds, status, pageable);
                } else {
                    pageResult = applicationRepository.findByApplicationTypeAndTargetIdIn("JOIN_ORG", myOrgIds, pageable);
                }
            }

            // 预处理目标ID集合以批量加载额外信息
            Set<Long> joinOrgIds = new HashSet<>();
            Set<Long> performanceIds = new HashSet<>();
            for (Application app : pageResult.getContent()) {
                if ("JOIN_ORG".equals(app.getApplicationType())) joinOrgIds.add(app.getTargetId());
                else if ("PERFORMANCE_APPLY".equals(app.getApplicationType())) performanceIds.add(app.getTargetId());
            }

            Map<Long, String> orgNameMap = new HashMap<>();
            if (!joinOrgIds.isEmpty()) {
                List<OrganizationInfo> orgs = organizationInfoRepository.findAllById(joinOrgIds);
                orgs.forEach(o -> orgNameMap.put(o.getId(), o.getName()));
            }

            Map<Long, Performance> performanceMap = new HashMap<>();
            Map<Long, String> organizerOrgNameMap = new HashMap<>();
            if (!performanceIds.isEmpty()) {
                List<Performance> performances = performanceRepository.findAllById(performanceIds);
                Set<Long> organizerOrgIds = new HashSet<>();
                for (Performance p : performances) {
                    performanceMap.put(p.getId(), p);
                    if ("ORGANIZATION".equals(p.getOrganizerType())) organizerOrgIds.add(p.getOrganizerId());
                }
                if (!organizerOrgIds.isEmpty()) {
                    List<OrganizationInfo> organizerOrgs = organizationInfoRepository.findAllById(organizerOrgIds);
                    organizerOrgs.forEach(o -> organizerOrgNameMap.put(o.getId(), o.getName()));
                }
            }

            // DTO 转换
            List<PendingApplicationDto> dtoList = new ArrayList<>();
            ObjectMapper objectMapper = new ObjectMapper();
            for (Application app : pageResult.getContent()) {
                PendingApplicationDto dto = new PendingApplicationDto();
                dto.setApplicationId(app.getId());
                dto.setApplicationType(app.getApplicationType());
                dto.setApplicantOpenId(app.getApplicant().getOpenid());
                dto.setApplicantName(app.getApplicant().getNickname());
                dto.setApplyTime(app.getApplyTime());
                dto.setStatus(app.getStatus());
                dto.setTargetId(app.getTargetId());
                dto.setExtraData(app.getExtraData());
                fillRichInfo(app, dto, orgNameMap, performanceMap, organizerOrgNameMap, objectMapper);
                dtoList.add(dto);
            }

            org.springframework.data.domain.Page<PendingApplicationDto> dtoPage = new org.springframework.data.domain.PageImpl<>(dtoList, pageable, pageResult.getTotalElements());
            return ApiResponse.success(dtoPage);

        } catch (Exception e) {
            logger.log(Level.WARNING, "申请查询失败", e);
            return ApiResponse.fail("申请查询失败: " + e.getMessage());
        }
    }

    // 兼容旧签名：调用分页版并返回 content 列表（若需要）
    public ApiResponse<List<PendingApplicationDto>> listApplications(@NotBlank String openId, String applicationType, Integer status) {
        ApiResponse<org.springframework.data.domain.Page<PendingApplicationDto>> pageResp = listApplications(openId, applicationType, status, 0, Integer.MAX_VALUE);
        if (!pageResp.isSuccess()) return ApiResponse.fail(pageResp.getMessage());
        org.springframework.data.domain.Page<PendingApplicationDto> page = pageResp.getData();
        return ApiResponse.success(page == null ? new ArrayList<>() : page.getContent());
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
                                                                       String applicationType,
                                                                       Integer status) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            List<Application> applications;
            if (applicationType != null && status != null) {
                applications = applicationRepository
                        .findByApplicantIdAndApplicationTypeAndStatusOrderByApplyTimeDesc(user.getId(), applicationType, status);
            } else if (applicationType != null) {
                applications = applicationRepository
                        .findByApplicantIdAndApplicationTypeOrderByApplyTimeDesc(user.getId(), applicationType);
            } else if (status != null) {
                applications = applicationRepository
                        .findByApplicantIdAndStatusOrderByApplyTimeDesc(user.getId(), status);
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

            if ("PERFORMANCE_APPLY".equals(application.getApplicationType())) {
                // 如果是演出申请，检查演出状态
                Performance performance = performanceRepository.findById(application.getTargetId())
                        .orElseThrow(() -> new IllegalArgumentException("关联的演出不存在"));
                if (performance.getPublishStatus() != 0) { // 0-待审核
                    throw new IllegalArgumentException("关联的演出已被审核，无法撤销申请");
                }

                performance.setPublishStatus(5); // 5-保存为草稿
                performanceRepository.save(performance);
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

    /**
     * 辅助方法：填充 DTO 的 targetName 和 applyUnitName
     */
    private void fillRichInfo(Application app,
                              PendingApplicationDto dto,
                              Map<Long, String> joinOrgNameMap,
                              Map<Long, Performance> performanceMap,
                              Map<Long, String> organizerOrgNameMap,
                              ObjectMapper objectMapper) {
        String type = app.getApplicationType();

        // 默认申请主体是“用户”
        dto.setApplyUnitType("USER");
        dto.setApplyUnitName(app.getApplicant().getNickname());
        dto.setTargetName("未知目标");

        try {
            if ("CREATE_ORG".equals(type)) {
                // 申请创建社团：目标名在 extraData
                JsonNode node = safeReadTree(app.getExtraData(), objectMapper);
                if (node != null && node.has("orgName")) {
                    dto.setTargetName(node.get("orgName").asText());
                }
            }
            else if ("JOIN_ORG".equals(type)) {
                // 申请加入社团：目标名是社团名
                dto.setTargetName(joinOrgNameMap.getOrDefault(app.getTargetId(), "未知社团"));
            }
            else if ("PERFORMANCE_APPLY".equals(type)) {
                // 演出申请：目标名是演出标题，申请主体可能是社团
                Performance p = performanceMap.get(app.getTargetId());
                if (p != null) {
                    dto.setTargetName(p.getTitle());

                    // 判断主办方类型
                    if ("ORGANIZATION".equals(p.getOrganizerType())) {
                        dto.setApplyUnitType("ORGANIZATION");
                        // 从预查询的 map 里拿社团名
                        String orgName = organizerOrgNameMap.getOrDefault(p.getOrganizerId(), "未知社团");
                        dto.setApplyUnitName(orgName);
                    } else {
                        // 个人申请：ApplyUnitName 就是 ApplicantName (User)，上面默认值已设置
                        dto.setApplyUnitType("USER");
                    }
                }
            }
        } catch (Exception e) {
            // 忽略非关键解析错误
        }
    }

    private JsonNode safeReadTree(String json, ObjectMapper om) {
        if (!StringUtils.hasText(json)) return null;
        try {
            return om.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
