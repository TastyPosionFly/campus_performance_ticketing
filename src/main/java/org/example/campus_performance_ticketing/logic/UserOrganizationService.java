package org.example.campus_performance_ticketing.logic;

import org.example.campus_performance_ticketing.dao.OrganizationRepository;
import org.example.campus_performance_ticketing.dao.UserOrganizationRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.Organization;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.model.UserOrganization;
import org.example.campus_performance_ticketing.util.JwtTokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserOrganizationService {

    private final UserOrganizationRepository userOrganizationRepository;
    private final OrganizationRepository organizationRepository;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserRepository userRepository;

    public UserOrganizationService(UserOrganizationRepository userOrganizationRepository,
                                   OrganizationRepository organizationRepository,
                                   JwtTokenUtil jwtTokenUtil,
                                   UserRepository userRepository) {
        this.userOrganizationRepository = userOrganizationRepository;
        this.organizationRepository = organizationRepository;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userRepository = userRepository;
    }

    /**
     * 用户申请加入组织
     */
    @Transactional
    public ApiResponse<UserOrganization> applyJoinOrganization(Long userId, Long organizationId) {
        try {
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            UserOrganization existing = userOrganizationRepository
                    .findByUserIdAndOrganizationId(userId, organizationId)
                    .orElse(null);

            if (existing != null) {
                return ApiResponse.fail("已申请或已加入该组织");
            }

            UserOrganization userOrg = new UserOrganization();
            userOrg.setUserId(userId);
            userOrg.setOrganizationId(organizationId);
            userOrg.setRole("PENDING"); // 待审批

            UserOrganization saved = userOrganizationRepository.save(userOrg);
            return ApiResponse.success(saved);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 组织首领审批加入请求
     */
    @Transactional
    public ApiResponse<UserOrganization> approveJoin(String token, Long userOrganizationId, boolean approve) {
        try {
            UserOrganization userOrg = userOrganizationRepository.findById(userOrganizationId)
                    .orElseThrow(() -> new RuntimeException("申请记录不存在"));

            Organization org = organizationRepository.findById(userOrg.getOrganizationId())
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            Long leaderId = ((Number) jwtTokenUtil.parseToken(token).get("userId")).longValue();

            if (!org.getLeaderUserId().equals(leaderId)) {
                return ApiResponse.fail("只有组织首领可以审批加入请求");
            }

            if (approve) {
                userOrg.setRole("MEMBER"); // 成员
                UserOrganization updated = userOrganizationRepository.save(userOrg);
                return ApiResponse.success(updated);
            } else {
                userOrganizationRepository.delete(userOrg); // 拒绝申请直接删除
                return ApiResponse.fail("已拒绝加入申请");
            }

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 首领踢出成员
     */
    @Transactional
    public ApiResponse<String> kickMember(String token, Long organizationId, Long memberId) {
        try {
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            Long leaderId = ((Number) jwtTokenUtil.parseToken(token).get("userId")).longValue();
            if (!org.getLeaderUserId().equals(leaderId)) {
                return ApiResponse.fail("只有组织首领可以踢出成员");
            }

            UserOrganization member = userOrganizationRepository
                    .findByUserIdAndOrganizationId(memberId, organizationId)
                    .orElseThrow(() -> new RuntimeException("该用户不是组织成员"));

            if ("PENDING".equals(member.getRole())) {
                return ApiResponse.fail("该用户尚未成为正式成员");
            }

            userOrganizationRepository.delete(member);
            return ApiResponse.success("成员已被踢出");

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 更换组织首领
     */
    @Transactional
    public ApiResponse<Organization> changeLeader(String token, Long organizationId, Long newLeaderId) {
        try {
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            Long currentLeaderId = ((Number) jwtTokenUtil.parseToken(token).get("userId")).longValue();
            if (!org.getLeaderUserId().equals(currentLeaderId)) {
                return ApiResponse.fail("只有当前首领可以更换组织首领");
            }

            // 检查新首领是否是组织成员
            UserOrganization newLeaderRelation = userOrganizationRepository
                    .findByUserIdAndOrganizationId(newLeaderId, organizationId)
                    .orElseThrow(() -> new RuntimeException("新首领必须是组织成员"));

            // 更新角色关系：旧首领降为 MEMBER
            UserOrganization oldLeaderRelation = userOrganizationRepository
                    .findByUserIdAndOrganizationId(currentLeaderId, organizationId)
                    .orElseThrow(() -> new RuntimeException("旧首领关系不存在"));
            oldLeaderRelation.setRole("MEMBER");
            userOrganizationRepository.save(oldLeaderRelation);

            // 更新组织首领
            org.setLeaderUserId(newLeaderId);
            UserOrganization newLeaderUpdated = newLeaderRelation;
            newLeaderUpdated.setRole("LEADER");
            userOrganizationRepository.save(newLeaderUpdated);

            Organization updatedOrg = organizationRepository.save(org);
            return ApiResponse.success(updatedOrg);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 获取组织所有成员详细信息
     */
    @Transactional
    public ApiResponse<List<UserInfo>> getOrganizationMembersInfo(Long organizationId) {
        try {
            Organization org = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new RuntimeException("组织不存在"));

            List<UserInfo> members = userRepository.findAllUsersByOrganizationId(organizationId);

            return ApiResponse.success(members);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
