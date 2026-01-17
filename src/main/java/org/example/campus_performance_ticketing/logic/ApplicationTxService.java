package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationAuditCommand;
import org.example.campus_performance_ticketing.model.Application;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.OrganizationMember;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.dao.ApplicationRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.OrganizationMemberRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.util.JsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ApplicationTxService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final JsonHelper jsonHelper;

    public ApplicationTxService(UserRepository userRepository,
                                ApplicationRepository applicationRepository,
                                OrganizationInfoRepository organizationInfoRepository,
                                OrganizationMemberRepository organizationMemberRepository,
                                JsonHelper jsonHelper) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.organizationInfoRepository = organizationInfoRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.jsonHelper = jsonHelper;
    }

    /**
     * 单条审核：独立事务，失败只回滚这一条，不影响其它条
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOneAudit(Long operatorId, ApplicationAuditCommand cmd) {

        UserInfo operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("操作人不存在"));

        Application app = applicationRepository.findById(cmd.getApplicationId())
                .orElseThrow(() -> new IllegalArgumentException("申请不存在"));

        if (app.getStatus() != 1) {
            throw new IllegalStateException("不是待审核");
        }

        // === 权限判断 ===
        if ("JOIN_ORG".equals(app.getApplicationType())) {
            OrganizationInfo organization = organizationInfoRepository.findById(app.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("目标组织不存在"));

            if (organization.getLeader() == null || organization.getLeader().getId() == null
                    || !organization.getLeader().getId().equals(operator.getId())) {
                throw new SecurityException("您不是目标组织的负责人，无权审核此申请");
            }
        } else {
            if (!"ADMIN".equals(operator.getRole()) && !"SUPER_ADMIN".equals(operator.getRole())) {
                throw new SecurityException("权限不足，只有管理员可以操作");
            }
        }

        // 通用设置
        app.setStatus(cmd.getNewStatus());
        app.setApproveTime(LocalDateTime.now());
        app.setApprover(operator);

        String oldExtraData = app.getExtraData();
        String newExtraData = jsonHelper.addReasonToJson(
                oldExtraData,
                cmd.getNewStatus() == 2 ? "同意理由" : "拒绝理由",
                cmd.getReason()
        );
        app.setExtraData(newExtraData);

        // 通过才执行业务
        if (cmd.getNewStatus() == 2) {
            handleApplicationPassBusiness(app, oldExtraData);
        }

        // 强制 flush：让数据库约束/外键等错误在“这一条事务里”立刻抛出
        applicationRepository.saveAndFlush(app);
    }

    /**
     * 审核通过后的具体业务逻辑
     * 注意：这里不要吞异常！让异常抛出去 -> 当前 REQUIRES_NEW 回滚即可
     */
    private void handleApplicationPassBusiness(Application app, String oldExtraData) {

        String type = app.getApplicationType();

        if ("CREATE_ORG".equals(type)) {
            String orgName;
            String orgDescription;
            String avatarUrl;

            try {
                JsonNode node = new ObjectMapper().readTree(oldExtraData);
                orgName = node.path("orgName").asText();
                orgDescription = node.path("orgDescription").asText();
                avatarUrl = node.path("avatarUrl").asText();
            } catch (Exception e) {
                throw new IllegalArgumentException("extraData 解析失败", e);
            }

            OrganizationInfo organization = new OrganizationInfo();
            organization.setName(orgName);
            organization.setDescription(orgDescription);
            organization.setAvatarUrl(avatarUrl);
            organization.setLeader(app.getApplicant());
            organization.setStatus(1);

            OrganizationInfo savedOrg = organizationInfoRepository.saveAndFlush(organization);

            OrganizationMember member = new OrganizationMember();
            member.setOrganization(savedOrg);
            member.setUser(app.getApplicant());
            member.setMemberRole("LEADER");
            member.setStatus(1);

            organizationMemberRepository.saveAndFlush(member);

            app.setTargetId(savedOrg.getId());
            return;
        }

        if ("JOIN_ORG".equals(type)) {
            OrganizationInfo organization = organizationInfoRepository.findById(app.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("目标组织不存在"));

            OrganizationMember member = new OrganizationMember();
            member.setOrganization(organization);
            member.setUser(app.getApplicant());
            member.setMemberRole("MEMBER");
            member.setStatus(1);

            organizationMemberRepository.saveAndFlush(member);
            return;
        }

        if ("DISBAND_ORG".equals(type)) {
            OrganizationInfo organizationInfo = organizationInfoRepository.findById(app.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("目标组织不存在"));

            organizationInfo.setStatus(3);

            // 强制 flush：如果 status/约束/触发器/乐观锁等有问题，会在这里抛出
            organizationInfoRepository.saveAndFlush(organizationInfo);
            return;
        }

        // 未处理类型：默认不做额外业务
    }
}