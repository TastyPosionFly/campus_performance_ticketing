package org.example.campus_performance_ticketing.logic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.ApplicationRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.OrganizationMemberRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.*;
import org.example.campus_performance_ticketing.logic.dto.organization.OrganizationMemberPublicDto;
import org.example.campus_performance_ticketing.logic.dto.organization.PublicOrganizationInfo;
import org.example.campus_performance_ticketing.logic.dto.organization.UserOrganizationMemberDto;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
import org.example.campus_performance_ticketing.model.Application;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.OrganizationMember;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
@Validated
@RequiredArgsConstructor

public class OrganizationMemberService {
    private final OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    private static final Logger logger = Logger.getLogger(OrganizationMemberService.class.getName());

    @Value("${file.base.url}")
    private String baseUrl;


    /**
     * 申请加入组织
     */
    @Transactional
    public ApiResponse<Void> applyJoinOrganization(@NotBlank String openId,
                                                   @NotNull Long orgId,
                                                   String reason) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationInfo organization = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            // 检查是否已是组织成员
            boolean isMember = organizationMemberRepository.findByOrganizationAndUser(organization, user).isPresent();
            if (isMember) {
                return ApiResponse.fail("你已经是该组织的成员，无需重复申请");
            }

            ObjectMapper om = new ObjectMapper();
            ObjectNode node = om.createObjectNode();
            node.put("userName", user.getNickname());
            node.put("orgName", organization.getName());
            node.put("reason", reason == null ? "" : reason);
            String extraData = om.writeValueAsString(node);

            Application application = new Application();
            application.setApplicant(user); // 如果你是Long类型用setApplicantId(user.getId());
            application.setApplicationType("JOIN_ORG");
            application.setExtraData(extraData);
            application.setTargetId(orgId);
            application.setStatus(1); // 1-待审核

            applicationRepository.save(application);

            ApiResponse<Void> response = ApiResponse.success(null);
            response.setMessage("申请已提交，等待审核");

            return response;
        } catch (Exception e) {
            logger.severe("申请加入组织失败: " + e.getMessage());
            return ApiResponse.fail("申请加入组织失败: " + e.getMessage());
        }
    }

    /**
     * 更改组织成员身份（LEADER）
     */
    @Transactional
    public ApiResponse<Void> changeOrganizationMemberRole(@NotBlank String openId,
                                                          @NotNull Long orgId,
                                                          @NotNull Long memberId,
                                                          @NotBlank String newRole) {
        try {
            UserInfo admin = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationInfo organizationInfo = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            // 此处应有权限检查逻辑，确保 admin 有权限更改成员身份
            if (admin == null || admin != organizationInfo.getLeader()) {
                return ApiResponse.fail("无权限更改成员身份");
            }

            OrganizationMember newMemberRole = organizationMemberRepository.findByOrganizationIdAndUserId(orgId, memberId)
                    .orElseThrow(() -> new IllegalArgumentException("组织成员不存在"));

            // 检查 newRole 是否合法
            if (!"MEMBER".equals(newRole) && !"MANAGER".equals(newRole) && !"LEADER".equals(newRole)) {
                return ApiResponse.fail("无效的成员身份");
            }

            if ("LEADER".equals(newRole)) {
                return ApiResponse.fail("不能直接将成员身份更改为首领");
            }

            newMemberRole.setMemberRole(newRole);
            organizationMemberRepository.save(newMemberRole);

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("成员身份更改成功");

            return resp;
        } catch (Exception e) {
            logger.warning("更改组织成员身份失败: " + e.getMessage());
            return ApiResponse.fail("更改组织成员身份失败: " + e.getMessage());
        }
    }

    /**
     * 退出组织
     */
    @Transactional
    public ApiResponse<Void> quitOrganization(@NotBlank String openId,
                                              @NotNull Long orgId) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationInfo organization = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            OrganizationMember member = organizationMemberRepository.findByOrganizationAndUser(organization, user)
                    .orElseThrow(() -> new IllegalArgumentException("用户不是该组织成员"));

            if (user == organization.getLeader()) {
                return ApiResponse.fail("组织首领不能直接退出组织，请先转让首领身份");
            }

            member.setStatus(2); // 2-已退出
            organizationMemberRepository.save(member);

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("成功退出组织");
            return resp;
        } catch (Exception e) {
            logger.warning("退出组织失败: " + e.getMessage());
            return ApiResponse.fail("退出组织失败: " + e.getMessage());
        }
    }

    /**
     * 踢出组织成员
     */
    @Transactional
    public ApiResponse<Void> kickOutOrganizationMember(@NotBlank String openId,
                                                       @NotNull Long orgId,
                                                       @NotNull Long memberId) {
        try {
            UserInfo admin = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            OrganizationInfo organization = organizationInfoRepository.findById(orgId)
                    .orElseThrow(() -> new IllegalArgumentException("组织不存在"));

            if (admin == null || admin != organization.getLeader()) {
                return ApiResponse.fail("无权限踢出成员");
            }
            OrganizationMember member = organizationMemberRepository.findByOrganizationIdAndUserId(orgId, memberId)
                    .orElseThrow(() -> new IllegalArgumentException("组织成员不存在"));

            member.setStatus(3); // 3-被踢出
            organizationMemberRepository.save(member);
            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("成功踢出组织成员");
            return resp;
        } catch (Exception e) {
            logger.warning("踢出组织成员失败: " + e.getMessage());
            return ApiResponse.fail("踢出组织成员失败: " + e.getMessage());
        }
    }


    /**
     * 定期物理删除已退出/被踢成员（例如 status = 2）
     * 每天凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional // 确保定时任务内是事务的
    public void deleteInactiveMembersPeriodically() {
        try {
            organizationMemberRepository.deleteByStatus(2);
            organizationMemberRepository.deleteByStatus(3);
            logger.info("定时清理已退出/被踢成员成功！");
        } catch (Exception e) {
            logger.severe("定时清理组织成员失败: " + e.getMessage());
        }
    }

    /**
     * 查看组织成员列表，仅返回公开信息
     */
    public ApiResponse<List<OrganizationMemberPublicDto>> listOrganizationMembers(Long orgId) {
        try {
            Iterable<OrganizationMember> members = organizationMemberRepository.findByOrganizationId(orgId);
            List<OrganizationMemberPublicDto> dtoList = new ArrayList<>();
            for (OrganizationMember member : members) {
                PublicUserInfo publicUser = new PublicUserInfo(
                        member.getUser().getNickname(),
                        AvatarUrlUtil.buildAvatarUrl(member.getUser().getAvatar(), baseUrl),
                        member.getUser().getMajor(),
                        member.getUser().getCollege(),
                        member.getUser().getStatus()
                );
                publicUser.setUserId(member.getUser().getId());

                OrganizationMemberPublicDto dto = new OrganizationMemberPublicDto();
                dto.setUser(publicUser);
                dto.setMemberRole(member.getMemberRole());
                dto.setStatus(member.getStatus());
                dtoList.add(dto);
            }
            ApiResponse<List<OrganizationMemberPublicDto>> resp = ApiResponse.success(dtoList);
            resp.setMessage("组织成员列表获取成功");
            return resp;
        } catch (Exception e) {
            logger.warning("获取组织成员列表失败: " + e.getMessage());
            return ApiResponse.fail("获取组织成员列表失败: " + e.getMessage());
        }
    }

    /**
     * 查看自己加入的组织列表
     */
    public ApiResponse<List<UserOrganizationMemberDto>> listUserOrganizations(@NotBlank String openId) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            Iterable<OrganizationMember> members = organizationMemberRepository.findByUserId(user.getId());
            List<UserOrganizationMemberDto> dtoList = new ArrayList<>();
            for (OrganizationMember member : members) {
                OrganizationInfo org = member.getOrganization();

                PublicUserInfo leader = new PublicUserInfo();
                leader.setNickname(org.getLeader().getNickname());
                leader.setAvatar(AvatarUrlUtil.buildAvatarUrl(org.getLeader().getAvatar(), baseUrl));
                leader.setMajor(org.getLeader().getMajor());
                leader.setCollege(org.getLeader().getCollege());
                leader.setStatus(org.getLeader().getStatus());

                PublicOrganizationInfo publicOrg = new PublicOrganizationInfo();
                publicOrg.setId(org.getId());
                publicOrg.setName(org.getName());
                publicOrg.setDescription(org.getDescription());
                publicOrg.setAvatarUrl(AvatarUrlUtil.buildAvatarUrl(org.getAvatarUrl(), baseUrl));
                publicOrg.setLeader(leader);
                publicOrg.setStatus(org.getStatus());

                UserOrganizationMemberDto dto = new UserOrganizationMemberDto();
                dto.setOrganization(publicOrg);
                dto.setMemberRole(member.getMemberRole());
                dto.setStatus(member.getStatus());
                dtoList.add(dto);
            }
            ApiResponse<List<UserOrganizationMemberDto>> resp = ApiResponse.success(dtoList);
            resp.setMessage("用户组织列表获取成功");
            return resp;
        } catch (Exception e) {
            logger.warning("获取用户组织列表失败: " + e.getMessage());
            return ApiResponse.fail("获取用户组织列表失败: " + e.getMessage());
        }
    }
}
