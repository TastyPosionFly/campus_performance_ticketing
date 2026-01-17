package org.example.campus_performance_ticketing.api;

import org.example.campus_performance_ticketing.logic.UserService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.user.LoginRequest;
import org.example.campus_performance_ticketing.logic.dto.user.LoginResult;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Value("${user.avatar.upload-dir}")
    private String avatarUploadDir;

    @Value("${file.base.url}")
    private String avatarBaseUrl;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 登录 / 注册（微信小程序） - 不需要 token
     */
    @PostMapping("/login")
    public ApiResponse<LoginResult> loginOrRegister(
            @RequestBody LoginRequest request
    ) {

        String avatarPath = null;

        // 先从数据库检查是否已有头像路径
        if (request.getOpenid() != null) {
            try {
                avatarPath = userService.getAvatarPathByOpenid(request.getOpenid());
            } catch (Exception e) {
                logger.error("获取头像路径失败，openid={}", request.getOpenid(), e);
                return ApiResponse.fail("获取头像失败：" + e.getMessage());
            }
        }

        if (request.getAvatar() != null && request.getAvatar().startsWith("http") && (avatarPath == null || avatarPath.isBlank())) {
            try {
                String safeDir = FileUtil.normalizeUploadDir(avatarUploadDir);

                // 使用 FileUtil 保存头像文件，返回文件保存路径
                avatarPath = FileUtil.saveAvatarFromUrl(request.getAvatar(), safeDir, ".jpg");
            } catch (Exception e) {
                return ApiResponse.fail("头像处理失败：" + e.getMessage());
            }
        }

        ApiResponse<LoginResult> response = userService.loginOrRegister(
                request.getOpenid(),
                request.getNickname(),
                avatarPath
        );

        if (response.getData() != null) {
            String avatar = response.getData().getAvatar();
            response.getData().setAvatar(AvatarUrlUtil.buildAvatarUrl(avatar, avatarBaseUrl));
        }

        return response;
    }
}