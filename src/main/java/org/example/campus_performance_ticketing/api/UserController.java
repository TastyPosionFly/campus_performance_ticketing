package org.example.campus_performance_ticketing.api;

import net.coobird.thumbnailator.Thumbnails;
import org.example.campus_performance_ticketing.logic.UserService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Value("${file.base.url}")
    private String avatarBaseUrl;

    @Value("${user.avatar.upload-dir}")
    private String avatarUploadDir;

    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserInfo> getCurrentUser(HttpServletRequest request) {
        String openId = (String) request.getAttribute("openid");
        return userService.getCurrentUser(openId);
    }

    /**
     * 获取指定用户信息
     */
    @GetMapping("/member")
    public ApiResponse<?> getUserInfo(
            HttpServletRequest request,
            @RequestParam(required = false) String openId
    ) {
        String role = (String) request.getAttribute("role");
        // 管理员和普通用户获取到的用户信息不同
        return userService.getMemberUserInfo(openId, role);
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    public ApiResponse<UserInfo> updateProfile(
            HttpServletRequest request,
            @RequestParam(required = false) String nickname,
            @RequestPart(value = "avatarFile", required = false) MultipartFile avatarFile,
            @RequestParam(required = false) Integer userIdentity,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String phone
    ) {

        // 从 token 获取 openId
        String openId = (String) request.getAttribute("openid");

        String avatarPath = null;

        // 处理头像文件上传
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String safeDir = FileUtil.normalizeUploadDir(avatarUploadDir);
                avatarPath = FileUtil.saveAvatar(avatarFile, safeDir, null);
            } catch (IOException e) {
                logger.severe("头像上传失败: " + e.getMessage());
                return ApiResponse.fail("头像上传失败：" + e.getMessage());
            }
        }

        // 调用 Service 层更新
        ApiResponse<UserInfo> response = userService.updateProfileByOpenId(
                openId, nickname, avatarPath, userIdentity, studentNo, major, college, phone
        );

        return response;
    }

}
