package org.example.campus_performance_ticketing.logic;

import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.LoginResult;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;



@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${user.avatar.upload-dir}")
    private String avatarUploadDir;

    public UserService(UserRepository userRepository, JwtTokenUtil jwtTokenUtil) {
        this.userRepository = userRepository;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * 微信登录 / 自动注册
     */
    @Transactional
    public ApiResponse<LoginResult> loginOrRegister(String openid,
                                                    String nickname,
                                                    String avatar) {
        if (!StringUtils.hasText(openid)) {
            return ApiResponse.fail("openid 不能为空");
        }

        try {
            UserInfo user = userRepository.findByOpenid(openid)
                    .orElseGet(() -> {
                        UserInfo newUser = new UserInfo();
                        newUser.setOpenid(openid);
                        newUser.setNickname(nickname);
                        newUser.setAvatar(avatar);
                        newUser.setStatus(1);        // 默认正常
                        newUser.setRole("USER");     // 默认角色
                        try {
                            return userRepository.save(newUser);
                        } catch (DataIntegrityViolationException ex) {
                            // 可能并发插入导致唯一约束，稍后会重新查询并返回
                            logger.warn("DataIntegrityViolation when saving new user for openid={}, will re-query", openid, ex);
                            return null;
                        }
                    });

            // 如果 orElseGet 的 save 因并发冲突返回 null，重新查询数据库
            if (user == null) {
                user = userRepository.findByOpenid(openid)
                        .orElseThrow(() -> new RuntimeException("用户注册后未找到，可能发生并发冲突"));
            }

            user.setLastLoginTime(LocalDateTime.now());

            // 更新头像（只有传入新的 avatar 且不同于现有值才更新）
            if (avatar != null && !avatar.equals(user.getAvatar())) {
                user.setAvatar(avatar);
            }

            user = userRepository.save(user);

            String token = jwtTokenUtil.generateTokenWithStatus(
                    user.getId(),
                    user.getOpenid(),
                    user.getRole(),
                    user.getStatus()
            );

            LoginResult result = new LoginResult(
                    token,
                    user.getId(),
                    user.getOpenid(),
                    user.getNickname(),
                    user.getAvatar(),
                    user.getRole(),
                    user.getStatus()
            );

            return ApiResponse.successWithToken(result, token);

        } catch (Exception e) {
            logger.error("loginOrRegister failed for openid={}", openid, e);
            return ApiResponse.fail("内部错误，登录失败");
        }
    }

        /**
         * 根据 openid 获取头像路径
         */
        public String getAvatarPathByOpenid(String openid) {
            if (!StringUtils.hasText(openid)) {
                return null;
            }
            try {
                String avatar_path = userRepository.findByOpenid(openid)
                        .map(UserInfo::getAvatar)
                        .orElse(null);

                // 检查该路径是否存在
                if (avatar_path != null) {
                    java.nio.file.Path path = java.nio.file.Paths.get("./" + avatar_path);
                    if (!java.nio.file.Files.exists(path)) {
                        return null;
                    }
                }

                return avatar_path;
            } catch (Exception e) {
                logger.warn("getAvatarPathByOpenid failed for openid={}", openid, e);
                return null;
            }
        }

    /**
     * 获取当前登录用户信息
     */
    public ApiResponse<UserInfo> getCurrentUser(String openId) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            return ApiResponse.success(user);

        } catch (Exception e) {
            logger.error("getCurrentUser failed for openId={}", openId, e);
            return ApiResponse.fail("获取用户信息失败");
        }
    }

    /**
     * 更新用户个人信息（昵称、头像、学号、专业等）
     * 仅允许本人操作 —> 请在 Controller / 过滤器中校验操作者与 openId 是否一致
     */
    @Transactional
    public ApiResponse<UserInfo> updateProfileByOpenId(String openId, String nickname, String avatar,
                                                       Integer userIdentity, String studentNo,
                                                       String major, String college, String phone) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }

        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (nickname != null) user.setNickname(nickname);
            if (avatar != null) user.setAvatar(avatar);
            if (userIdentity != null) user.setUserIdentity(userIdentity);
            if (studentNo != null) user.setStudentNo(studentNo);
            if (major != null) user.setMajor(major);
            if (college != null) user.setCollege(college);
            if (phone != null) user.setPhone(phone);

            UserInfo updated = userRepository.save(user);
            return ApiResponse.success(updated);
        } catch (Exception e) {
            logger.error("updateProfileByOpenId failed for openId={}", openId, e);
            return ApiResponse.fail("更新用户信息失败");
        }
    }


    /**
     * 封禁 / 解封用户
     * 仅管理员操作 —> 建议从安全上下文获取角色，而不是信任传入的参数
     */
    @Transactional
    public ApiResponse<UserInfo> banOrUnbanUser(String openId, boolean ban, String role) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }

        try {
            if (role == null || (!"ADMIN".equalsIgnoreCase(role) && !"SUPER_ADMIN".equalsIgnoreCase(role))) {
                return ApiResponse.fail("没有权限操作用户封禁");
            }

            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            user.setStatus(ban ? 0 : 1); // 0=封禁, 1=正常
            UserInfo updated = userRepository.save(user);

            // 注意：若系统使用 JWT，需要确保在 token 校验时验证用户状态（或在此引入 token 失效机制）
            return ApiResponse.success(updated);

        } catch (Exception e) {
            logger.error("banOrUnbanUser failed for openId={}, ban={}", openId, ban, e);
            return ApiResponse.fail("操作失败");
        }
    }


    /**
     * 根据ID查询用户信息
     */
    public ApiResponse<UserInfo> getUserByOpenId(String openId) {
        if (!StringUtils.hasText(openId)) {
            return ApiResponse.fail("openId 不能为空");
        }
        try {
            Optional<UserInfo> userOpt = userRepository.findByOpenid(openId);
            logger.debug("查询用户 openId = {}", openId);
            if (userOpt.isPresent()) {
                return ApiResponse.success(userOpt.get());
            } else {
                return ApiResponse.fail("用户不存在");
            }
        } catch (Exception e) {
            logger.error("getUserByOpenId failed for openId={}", openId, e);
            return ApiResponse.fail("查询用户失败");
        }
    }
}
