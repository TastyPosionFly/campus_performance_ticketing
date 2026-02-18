package org.example.campus_performance_ticketing.logic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.campus_performance_ticketing.dao.OrganizationInfoRepository;
import org.example.campus_performance_ticketing.dao.UserRepository;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.user.LoginResult;
import org.example.campus_performance_ticketing.logic.dto.user.PublicUserInfo;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.example.campus_performance_ticketing.util.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Validated
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OrganizationInfoRepository organizationInfoRepository;
    private final JwtTokenUtil jwtTokenUtil;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Value("${file.base.url}")
    private String baseUrl;

    /**
     * 检查是否为管理员
     */
    private boolean isAdmin(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "SUPER_ADMIN".equalsIgnoreCase(role);
    }

    /**
     * 检查是否为超级管理员
     */
    private boolean isSuperAdmin(String role) {
        return "SUPER_ADMIN".equalsIgnoreCase(role);
    }

    /**
     * 微信登录 / 自动注册
     */
    @Transactional
    public ApiResponse<LoginResult> loginOrRegister(@NotBlank String openid,
                                                    String nickname,
                                                    String avatar) {
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
                    user.getStatus(),
                    organizationInfoRepository.existsByLeaderId(user.getId())
            );

            return ApiResponse.successWithToken(result, token);

        } catch (Exception e) {
            logger.error("loginOrRegister failed for openid={}", openid, e);
            return ApiResponse.fail("内���错误，登录失败");
        }
    }

    /**
     * 根据 openid 获取头像路径
     */
    public String getAvatarPathByOpenid(@NotBlank String openid) {
        try {
            String avatar_path = userRepository.findByOpenid(openid)
                    .map(UserInfo::getAvatar)
                    .orElse(null);

            // 检查该路径是否存在
            if (avatar_path != null) {
                java.nio.file.Path path = java.nio.file.Paths.get(avatar_path);
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
    public ApiResponse<UserInfo> getCurrentUser(@NotBlank String openId) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            user.setAvatar(AvatarUrlUtil.buildAvatarUrl(user.getAvatar(), baseUrl));

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
    public ApiResponse<UserInfo> updateProfileByOpenId(@NotBlank String openId,
                                                       String nickname, String avatar,
                                                       Integer userIdentity, String studentNo,
                                                       String major, String college, String phone) {
        try {
            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (nickname != null) user.setNickname(nickname);
            // 如果上传了最新的图片，则将老图片在服务器中删除
            if (avatar != null) {
                String oldAvatar = user.getAvatar();
                if (oldAvatar != null) {
                    FileUtil.deletePhysicalFile(oldAvatar);
                }
                user.setAvatar(avatar);
            }
            if (userIdentity != null) user.setUserIdentity(userIdentity);
            if (studentNo != null) user.setStudentNo(studentNo);
            if (major != null) user.setMajor(major);
            if (college != null) user.setCollege(college);
            if (phone != null) user.setPhone(phone);

            UserInfo updated = userRepository.save(user);

            // 构造返回对象
            UserInfo respUser = new UserInfo();
            respUser.setId(updated.getId());
            respUser.setNickname(updated.getNickname());
            respUser.setUserIdentity(updated.getUserIdentity());
            respUser.setStudentNo(updated.getStudentNo());
            respUser.setMajor(updated.getMajor());
            respUser.setCollege(updated.getCollege());
            respUser.setPhone(updated.getPhone());
            respUser.setStatus(updated.getStatus());
            respUser.setAvatar(AvatarUrlUtil.buildAvatarUrl(updated.getAvatar(), baseUrl));

            return ApiResponse.success(respUser);
        } catch (Exception e) {
            logger.error("updateProfileByOpenId failed for openId={}", openId, e);
            return ApiResponse.fail("更新用户信息失败");
        }
    }

    /**
     * 查询用户是否存在
     */
    @Transactional(readOnly = true)
    public UserInfo findUserByOpenId(@NotBlank String openId) {
        try {
            return userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        } catch (Exception e) {
            logger.error("用户不存在{}", openId, e);
            throw e;
        }
    }

    /**
     * 封禁 / 解封用户（含权限校验）
     */
    @Transactional
    public ApiResponse<UserInfo> banOrUnbanUser(@NotBlank String openId,
                                                @NotNull Boolean ban,
                                                @NotBlank String operatorRole) {
        try {
            // 权限校验：只有管理员可以操作
            if (!isAdmin(operatorRole)) {
                return ApiResponse.fail("没有权限操作用户封禁");
            }

            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            user.setStatus(ban ? 0 : 1); // 0=封禁, 1=正常
            UserInfo updated = userRepository.save(user);

            logger.info("用户 {} 被 {} {}，操作者角色：{}", openId, ban ? "封禁" : "解封", updated.getStatus(), operatorRole);

            return ApiResponse.success(updated);

        } catch (Exception e) {
            logger.error("banOrUnbanUser failed for openId={}, ban={}", openId, ban, e);
            return ApiResponse.fail("操作失败");
        }
    }

    /**
     * 获取所有用户列表（含权限校验）
     */
    @Transactional(readOnly = true)
    public ApiResponse<Iterable<UserInfo>> getAllUsers(@NotBlank String operatorRole) {
        try {
            // 权限校验：只有管理员可以查看用户列表
            if (!isAdmin(operatorRole)) {
                return ApiResponse.fail("没有权限查看用户列表");
            }

            Iterable<UserInfo> users = userRepository.findAll();

            // 处理头像URL
            users.forEach(user -> {
                user.setAvatar(AvatarUrlUtil.buildAvatarUrl(user.getAvatar(), baseUrl));
            });

            return ApiResponse.success(users);
        } catch (Exception e) {
            logger.error("getAllUsers failed", e);
            return ApiResponse.fail("查询用户列表失败");
        }
    }

    /**
     * 分页获取所有用户（ADMIN / SUPER_ADMIN）
     * @param operatorRole 当前操作者角色
     * @param page 页码（0-based）
     * @param size 每页大小
     */
    @Transactional(readOnly = true)
    public ApiResponse<org.springframework.data.domain.Page<UserInfo>> getAllUsersPaginated(@NotBlank String operatorRole, int page, int size) {
        try {
            if (!isAdmin(operatorRole)) {
                return ApiResponse.fail("没有权限查看用户列表");
            }

            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(Math.max(0, page), Math.max(1, size));
            org.springframework.data.domain.Page<UserInfo> users = userRepository.findAll(pageable);

            // 处理头像 URL
            users.forEach(user -> user.setAvatar(AvatarUrlUtil.buildAvatarUrl(user.getAvatar(), baseUrl)));

            return ApiResponse.success(users);
        } catch (Exception e) {
            logger.error("getAllUsersPaginated failed", e);
            return ApiResponse.fail("分页查询用户列表失败");
        }
    }

    /**
     * 根据角色返回不同层级的用户信息
     */
    public ApiResponse<?> getMemberUserInfo(@NotNull Long id,
                                            @NotBlank String role) {
        try {
            if (isAdmin(role)) {
                // 管理员返回全部信息
                UserInfo response = userRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

                response.setAvatar(AvatarUrlUtil.buildAvatarUrl(response.getAvatar(), baseUrl));

                return ApiResponse.success(response);
            } else {
                // 普通用户返回公开信息
                Optional<PublicUserInfo> publicInfoOpt = userRepository.findPublicUserInfoById(id);
                if (publicInfoOpt.isPresent()) {
                    PublicUserInfo info = publicInfoOpt.get();
                    info.setAvatar(AvatarUrlUtil.buildAvatarUrl(info.getAvatar(), baseUrl));
                    return ApiResponse.success(info);
                } else {
                    return ApiResponse.fail("用户不存在");
                }
            }
        } catch (Exception e) {
            logger.error("getMemberUserInfo failed for openId={}", id, e);
            return ApiResponse.fail("查询用户信息失败");
        }
    }

    /**
     * 修改用户角色（含权限校验）
     */
    @Transactional
    public ApiResponse<UserInfo> updateUserRole(@NotBlank String openId,
                                                @NotBlank String newRole,
                                                @NotBlank String operatorRole) {
        try {
            // 权限校验：只有超级管理员可以修改角色
            if (!isSuperAdmin(operatorRole)) {
                return ApiResponse.fail("没有权限修改用户角色");
            }

            // 角色白名单校验
            if (!isValidRole(newRole)) {
                return ApiResponse.fail("角色参数不合法，只能为 USER、VENUE_ADMIN、ADMIN、SUPER_ADMIN");
            }

            UserInfo user = userRepository.findByOpenid(openId)
                    .orElseThrow(() -> new RuntimeException("用户不存在"));

            String oldRole = user.getRole();
            user.setRole(newRole);
            UserInfo updated = userRepository.save(user);

            logger.info("用户 {} 角色从 {} 变更为 {}，操作者角色：{}", openId, oldRole, newRole, operatorRole);

            return ApiResponse.success(updated);
        } catch (Exception e) {
            logger.error("updateUserRole failed for openId={}, newRole={}", openId, newRole, e);
            return ApiResponse.fail("修改用户角色失败");
        }
    }

    /**
     * 根据用户名精确查询用户（含权限校验）
     */
    @Transactional(readOnly = true)
    public ApiResponse<?> findUserByNickname(@NotBlank String nickname, @NotBlank String operatorRole) {
        try {
            // 权限校验：只有管理员可以查询用户
            if (!isAdmin(operatorRole)) {
                return ApiResponse.fail("没有权限查询用户信息");
            }

            Optional<UserInfo> userOpt = userRepository.findByNickname(nickname);

            if (userOpt.isEmpty()) {
                return ApiResponse.fail("用户不存在");
            }

            UserInfo user = userOpt.get();
            user.setAvatar(AvatarUrlUtil.buildAvatarUrl(user.getAvatar(), baseUrl));

            return ApiResponse.success(user);
        } catch (Exception e) {
            logger.error("findUserByNickname failed for nickname={}", nickname, e);
            return ApiResponse.fail("查询用户失败");
        }
    }

    /**
     * 根据用户名模糊搜索用户列表（含权限校验）
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<UserInfo>> searchUsersByNickname(@NotBlank String keyword, @NotBlank String operatorRole,
                                                              String userRole) {
         try {
             // 权限校验：只有管理员可以搜索用户列表
             if (!isAdmin(operatorRole)) {
                 return ApiResponse.fail("没有权限搜索用户");
             }

             List<UserInfo> users;

             if (userRole != null && !isValidRole(userRole)) {
                 return ApiResponse.fail("用户角色参数不合法，只能为 USER、VENUE_ADMIN、ADMIN、SUPER_ADMIN");
             }

             if (userRole == null) {
                 users = userRepository.findByNicknameContaining(keyword);
             } else {
                 users = userRepository.findByNicknameContainingAndRole(keyword, userRole);
             }

             // 处理头像URL
             users.forEach(user -> {
                 user.setAvatar(AvatarUrlUtil.buildAvatarUrl(user.getAvatar(), baseUrl));
             });

             return ApiResponse.success(users);
         } catch (Exception e) {
             logger.error("searchUsersByNickname failed for keyword={}", keyword, e);
             return ApiResponse.fail("搜索用户失败");
         }
     }

    /**
     * 分页模糊搜索用户（管理员权限）
     */
    @Transactional(readOnly = true)
    public ApiResponse<Page<UserInfo>> searchUsersByNicknamePaginated(@NotBlank String keyword, @NotBlank String operatorRole,
                                                                                                      String userRole, int page, int size) {
        try {
            if (!isAdmin(operatorRole)) {
                return ApiResponse.fail("没有权限搜索用户");
            }

            if (userRole != null && !isValidRole(userRole)) {
                return ApiResponse.fail("用户角色参数不合法，只能为 USER、VENUE_ADMIN、ADMIN、SUPER_ADMIN");
            }

            Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
            Page<UserInfo> result;

            if (userRole == null) {
                result = userRepository.findByNicknameContaining(keyword, pageable);
            } else {
                result = userRepository.findByNicknameContainingAndRole(keyword, userRole, pageable);
            }

            // Map avatars
            result.forEach(u -> u.setAvatar(AvatarUrlUtil.buildAvatarUrl(u.getAvatar(), baseUrl)));

            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("searchUsersByNicknamePaginated failed for keyword={}", keyword, e);
            return ApiResponse.fail("搜索用户失败");
        }
    }

    /**
     * 用户角色枚举
     */
    public enum UserRoleEnum {
        USER, VENUE_ADMIN, ADMIN, SUPER_ADMIN
    }

    /**
     * 角色白名单校验
     */
    private static boolean isValidRole(String role) {
        try {
            UserRoleEnum.valueOf(role);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}

