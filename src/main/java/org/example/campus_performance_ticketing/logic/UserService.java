package org.example.campus_performance_ticketing.logic;

import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.LoginResult;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.JwtTokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

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
        try {
            UserInfo user = userRepository.findByOpenid(openid)
                    .orElseGet(() -> {
                        UserInfo newUser = new UserInfo();
                        newUser.setOpenid(openid);
                        newUser.setNickname(nickname);
                        newUser.setAvatar(avatar);
                        newUser.setStatus(1);        // ✅ 默认正常
                        newUser.setRole("USER");     // ✅ 默认角色
                        return userRepository.save(newUser);
                    });

            user.setLastLoginTime(LocalDateTime.now());
            userRepository.save(user);

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
            return ApiResponse.fail(e.getMessage());
        }
    }



    /**
     * 获取当前登录用户信息
     */
    public ApiResponse<UserInfo> getCurrentUser(String openId) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            return ApiResponse.success(user);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 更新用户个人信息（昵称、头像、学号、专业等）
     * 仅允许本人操作
     */
    @Transactional
    public ApiResponse<UserInfo> updateProfileByOpenId(String openId, String nickname, String avatar,
                                                       Integer userIdentity, String studentNo,
                                                       String major, String college, String phone) {
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
    }


    /**
     * 封禁 / 解封用户
     * 仅管理员操作
     */
    @Transactional
    public ApiResponse<UserInfo> banOrUnbanUser(String openId, boolean ban, String role) {
        try {
            if (!"ADMIN".equalsIgnoreCase(role) && !"SUPER_ADMIN".equalsIgnoreCase(role)) {
                return ApiResponse.fail("没有权限操作用户封禁");
            }

            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            user.setStatus(ban ? 0 : 1); // 0=封禁, 1=正常
            UserInfo updated = userRepository.save(user);
            return ApiResponse.success(updated);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }


    /**
     * 根据ID查询用户信息
     */
    public ApiResponse<UserInfo> getUserByOpenId(String openId) {
        try {
            Optional<UserInfo> userOpt = userRepository.findByOpenid(openId);
            System.out.println("收到 openId = " + openId);
            if (userOpt.isPresent()) {
                return ApiResponse.success(userOpt.get());
            } else {
                return ApiResponse.fail("用户不存在");
            }
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage());
        }
    }
}
