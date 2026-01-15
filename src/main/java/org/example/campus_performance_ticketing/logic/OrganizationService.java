package org.example.campus_performance_ticketing.logic;

import org.example.campus_performance_ticketing.dao.ApplicationRepository;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.Application;
import org.example.campus_performance_ticketing.model.OrganizationInfo;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.logging.Logger;

/**
 * 组织服务类
 */
@Service
public class OrganizationService {

    private final OrganizationInfoRepository organizationInfoRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;

    private static final Logger logger = Logger.getLogger(OrganizationService.class.getName());

    public OrganizationService(OrganizationInfoRepository organizationInfoRepository,
                               UserRepository userRepository,
                               ApplicationRepository applicationRepository) {
        this.organizationInfoRepository = organizationInfoRepository;
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
    }

    /**
     * 申请创建组织
     */
    @Transactional
    public ApiResponse<Void> applyCreateOrganization(String openId,
                                                     String orgName,
                                                     String orgDescription) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            // 构造 extraData JSON（推荐用fastjson、Jackson或手工拼接）
            String extraData = String.format("{\"orgName\":\"%s\",\"orgDescription\":\"%s\"}",
                    orgName.replace("\"", "\\\""),
                    orgDescription == null ? "" : orgDescription.replace("\"", "\\\""));

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
     * 撤销个人创建组织申请
     */
    @Transactional
    public ApiResponse<Void> revokeCreateOrganizationApplication(String openId, Long applicationId) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }

        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            Application application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new IllegalArgumentException("申请不存在"));

            if (!"CREATE_ORG".equals(application.getApplicationType())) {
                return ApiResponse.fail("该申请不是创建组织类型，无法撤销");
            }


            if (!application.getApplicant().getId().equals(user.getId())) {
                throw new IllegalArgumentException("只能撤销自己的申请");
            }

            if (application.getStatus() != 1) {
                throw new IllegalArgumentException("只能撤销待审核的申请");
            }

            application.setStatus(4); // 4-撤销
            applicationRepository.save(application);

            ApiResponse<Void> resp = ApiResponse.success(null);
            resp.setMessage("成功撤销创建组织申请");
            return resp;
        } catch (Exception e) {
            logger.warning("撤销创建组织申请失败: " + e.getMessage());
            return ApiResponse.fail("撤销创建组织申请失败: " + e.getMessage());
        }
    }


}
