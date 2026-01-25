package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.*;
import org.example.campus_performance_ticketing.logic.dto.application.ApplicationAuditCommand;
import org.example.campus_performance_ticketing.model.*;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.example.campus_performance_ticketing.util.JsonHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;

@Service
@RequiredArgsConstructor
public class ApplicationTxService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    // 新增：引入演出仓库
    private final PerformanceRepository performanceRepository;
    private final JsonHelper jsonHelper;

    private static final Logger logger = Logger.getLogger(ApplicationTxService.class.getName());

    // === 注入正式目录配置 (用于文件移动) ===
    @Value("${performance.post.upload-dir}")
    private String posterRealDir;

    @Value("${staff.photo.upload-dir}")
    private String staffRealDir;

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
            throw new IllegalStateException("申请状态已变更，不是待审核状态");
        }

        // === 权限校验 ===
        checkAuditPermission(operator, app);

        // === 状态更新 ===
        app.setStatus(cmd.getNewStatus());
        app.setApproveTime(LocalDateTime.now());
        app.setApprover(operator);

        // 更新 JSON 中的审核意见
        String oldExtraData = app.getExtraData();
        String newExtraData = jsonHelper.addReasonToJson(
                oldExtraData,
                cmd.getNewStatus() == 2 ? "同意理由" : "拒绝理由",
                cmd.getReason()
        );
        app.setExtraData(newExtraData);

        // === 业务回调 (仅通过时执行) ===
        if (cmd.getNewStatus() == 2) {
            // 审核通过
            handleApplicationPassBusiness(app, oldExtraData);
        } else if (cmd.getNewStatus() == 3) {
            // 审核拒绝 (3是Application的拒绝状态)
            handleApplicationRejectBusiness(app);
        }

        // 强制 flush：让数据库约束/外键等错误在“这一条事务里”立刻抛出
        applicationRepository.saveAndFlush(app);
    }

    /**
     * 权限校验逻辑抽取
     */
    private void checkAuditPermission(UserInfo operator, Application app) {
        // 社团加入申请：由社长审核
        if ("JOIN_ORG".equals(app.getApplicationType())) {
            OrganizationInfo organization = organizationInfoRepository.findById(app.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("目标组织不存在"));

            if (organization.getLeader() == null || !organization.getLeader().getId().equals(operator.getId())) {
                throw new SecurityException("您不是目标组织的负责人，无权审核此申请");
            }
        }
        // 其它申请 (创建社团、解散社团、演出申请)：由管理员审核
        else {
            if (!"ADMIN".equals(operator.getRole()) && !"SUPER_ADMIN".equals(operator.getRole())) {
                throw new SecurityException("权限不足，只有管理员可以操作");
            }
        }
    }

    /**
     * 审核通过后的具体业务逻辑
     */
    private void handleApplicationPassBusiness(Application app, String oldExtraData) {
        String type = app.getApplicationType();

        switch (type) {
            case "CREATE_ORG" -> handleCreateOrg(app, oldExtraData);
            case "JOIN_ORG" -> handleJoinOrg(app);
            case "DISBAND_ORG" -> handleDisbandOrg(app);
            case "PERFORMANCE_APPLY" -> activatePerformance(app);
            default -> {
                // 未处理类型：默认不做额外业务
            }
        }
    }

    /**
     * 审核拒绝后的具体业务逻辑
     * @param app
     */
    private void handleApplicationRejectBusiness(Application app) {
        // 仅处理演出申请的联动拒绝
        if ("PERFORMANCE_APPLY".equals(app.getApplicationType())) {
            Performance performance = performanceRepository.findById(app.getTargetId())
                    .orElseThrow(() -> new IllegalArgumentException("演出不存在"));

            performance.setPublishStatus(4); // 4-审批拒绝

            performanceRepository.saveAndFlush(performance);
        }
    }

    // === 具体业务逻辑拆分 ===

    /**
     * 激活演出：移动临时文件 -> 正式目录，并更新状态
     * 逻辑完全在此处闭环，不依赖 PerformanceService
     */
    private void activatePerformance(Application app) {
        Performance performance = performanceRepository.findById(app.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("演出不存在"));

        // 1. 移动海报
        if (isTempPath(performance.getPosterUrl())) {
            try {
                String newPath = FileUtil.moveFile(performance.getPosterUrl(), posterRealDir);
                performance.setPosterUrl(newPath);
            } catch (IOException e) {
                // 抛出运行时异常以触发事务回滚
                throw new RuntimeException("审批失败：海报移动异常", e);
            }
        }

        // 2. 移动演职人员图片
        List<PerformanceStaff> staffList = performance.getStaffList();
        if (staffList != null) {
            for (PerformanceStaff staff : staffList) {
                if (isTempPath(staff.getStaffAvatar())) {
                    try {
                        String newPath = FileUtil.moveFile(staff.getStaffAvatar(), staffRealDir);
                        staff.setStaffAvatar(newPath);
                    } catch (IOException e) {
                        logger.warning("人员照片移动失败: " + staff.getStaffName());
                        // 策略：人员照片失败不阻断整个流程，或者也可以选择抛异常
                    }
                }
            }
        }

        // 3. 上架
        performance.setPublishStatus(1);
        performanceRepository.saveAndFlush(performance);
    }

    private void handleCreateOrg(Application app, String extraData) {
        try {
            JsonNode node = new ObjectMapper().readTree(extraData);
            String orgName = node.path("orgName").asText();
            String orgDescription = node.path("orgDescription").asText();
            String avatarUrl = node.path("avatarUrl").asText();

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

            // 回写 targetId
            app.setTargetId(savedOrg.getId());
        } catch (Exception e) {
            throw new IllegalArgumentException("创建组织失败: " + e.getMessage(), e);
        }
    }

    private void handleJoinOrg(Application app) {
        OrganizationInfo organization = organizationInfoRepository.findById(app.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("目标组织不存在"));

        OrganizationMember member = new OrganizationMember();
        member.setOrganization(organization);
        member.setUser(app.getApplicant());
        member.setMemberRole("MEMBER");
        member.setStatus(1);

        organizationMemberRepository.saveAndFlush(member);
    }

    private void handleDisbandOrg(Application app) {
        OrganizationInfo organizationInfo = organizationInfoRepository.findById(app.getTargetId())
                .orElseThrow(() -> new IllegalArgumentException("目标组织不存在"));

        organizationInfo.setStatus(2); // 2-已解散
        organizationInfoRepository.saveAndFlush(organizationInfo);
    }

    private boolean isTempPath(String path) {
        return path != null && path.contains("/temp/");
    }
}