package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.campus_performance_ticketing.dao.ApplicationRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.OrganizationMemberRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.*;
import org.example.campus_performance_ticketing.model.Application;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.OrganizationMember;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.JsonHelper;
import org.example.campus_performance_ticketing.util.JwtTokenUtil;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 申请服务类
 */

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final OrganizationMemberRepository organizationMemberRepository;

    private final JsonHelper jsonHelper;

    private final static Logger logger = Logger.getLogger(ApplicationService.class.getName());

    public ApplicationService (ApplicationRepository applicationRepository,
                               UserRepository userRepository,
                               OrganizationInfoRepository organizationInfoRepository,
                               OrganizationMemberRepository organizationMemberRepository,
                               JsonHelper jsonHelper
                               ) {
        this.applicationRepository = applicationRepository;

        this.userRepository = userRepository;
        this.organizationInfoRepository = organizationInfoRepository;
        this.organizationMemberRepository = organizationMemberRepository;

        this.jsonHelper = jsonHelper;
    }

    /**
     * 查询所有待审核的创建组织申请信息
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<PendingApplicationDto>> listApplications(String openId, String applicationType, Integer status) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }
        try {
            UserInfo userInfo = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            if (!"ADMIN".equals(userInfo.getRole()) && !"SUPER_ADMIN".equals(userInfo.getRole())) {
                return ApiResponse.fail("权限不足，只有管理员可查看申请");
            }

            List<Application> applicationList;
            if (applicationType != null && status != null) {
                applicationList = applicationRepository.findByApplicationTypeAndStatus(applicationType, status);
            } else if (applicationType != null) {
                applicationList = applicationRepository.findByApplicationType(applicationType);
            } else if (status != null) {
                applicationList = applicationRepository.findByStatus(status);
            } else {
                applicationList = applicationRepository.findAll();
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
                // 通用展示字段的解析（可做扩展）
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


    @Transactional
    public ApiResponse<Void> batchChangeApplicationStatus(String openId, List<ApplicationAuditCommand> audits) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }
        if (audits == null || audits.isEmpty()) {
            return ApiResponse.fail("操作列表不能为空");
        }
        try {
            UserInfo admin = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            if (!"ADMIN".equals(admin.getRole()) && !"SUPER_ADMIN".equals(admin.getRole())) {
                return ApiResponse.fail("权限不足，只有管理员可以操作");
            }

            int successCount = 0;
            int failCount = 0;
            StringBuilder failMsgs = new StringBuilder();

            for (ApplicationAuditCommand cmd : audits) {
                if (cmd == null || cmd.getApplicationId() == null
                        || cmd.getNewStatus() == null
                        || !(cmd.getNewStatus() == 2 || cmd.getNewStatus() == 3)
                        || !StringUtils.hasText(cmd.getReason())) {
                    failCount++;
                    failMsgs.append("参数异常;");
                    continue;
                }
                try {
                    Application app = applicationRepository.findById(cmd.getApplicationId())
                            .orElseThrow(() -> new IllegalArgumentException("申请不存在"));

                    if (app.getStatus() != 1) {
                        failCount++;
                        failMsgs.append(String.format("申请%d:不是待审核;", cmd.getApplicationId()));
                        continue;
                    }
                    // 通用设置
                    app.setStatus(cmd.getNewStatus());
                    app.setApproveTime(java.time.LocalDateTime.now());
                    app.setApprover(admin);
                    String oldExtraData = app.getExtraData();
                    String newExtraData = jsonHelper.addReasonToJson(oldExtraData,
                            cmd.getNewStatus() == 2 ? "同意理由" : "拒绝理由", cmd.getReason());
                    app.setExtraData(newExtraData);

                    if (cmd.getNewStatus() == 2) {
                        boolean pass = handleApplicationPassBusiness(app, oldExtraData, failMsgs);
                        if (!pass) {
                            failCount++;
                            continue;
                        }
                    }

                    applicationRepository.save(app);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    failMsgs.append(String.format("申请%d: %s;", cmd.getApplicationId(), e.getMessage()));
                }
            }

            String msg = String.format("审核成功%d条，失败%d条。%s", successCount, failCount, failMsgs.toString());
            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage(msg);
            return resp;

        } catch (Exception e) {
            logger.warning("批量更改申请状态失败: " + e.getMessage());
            return ApiResponse.fail("批量更改申请状态失败: " + e.getMessage());
        }
    }


    /**
     * 保存申请记录
     */
    @Transactional
    public Application saveApplication(Application application) {
        try {
            return applicationRepository.save(application);
        } catch (Exception e){
            logger.severe("保存申请记录失败: " + e.getMessage());
            throw e;
        }
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
            String orgName = "", orgDescription = "";
            try {
                JsonNode node = new ObjectMapper().readTree(oldExtraData);
                orgName = node.path("orgName").asText();
                orgDescription = node.path("orgDescription").asText();
            } catch (Exception e) {
                failMsgs.append(String.format("申请%d:extraData 解析失败;", app.getId()));
                return false;
            }
            OrganizationInfo organization = new OrganizationInfo();
            organization.setName(orgName);
            organization.setDescription(orgDescription);
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

        // 可扩展其它类型：如申请加入组织、申请编辑相册等
        // else if ("JOIN_ORG".equals(type)) { ... }

        // 未处理类型
        return true;
    }

    /**
     * 查询个人申请记录（仅返回公开信息）
     */
    public ApiResponse<List<ApplicationPublicDto>> getUserApplications(String openId) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            List<Application> applications = applicationRepository.findByApplicantIdOrderByApplyTimeDesc(user.getId());

            List<ApplicationPublicDto> dtoList = new ArrayList<>();
            for (Application app : applications) {
                ApplicationPublicDto dto = new ApplicationPublicDto();
                dto.setId(app.getId());

                // 只赋值公开信息
                UserInfo applicant = app.getApplicant();
                PublicUserInfo publicUser = new PublicUserInfo(
                        applicant.getNickname(),
                        applicant.getAvatar(),
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
}
