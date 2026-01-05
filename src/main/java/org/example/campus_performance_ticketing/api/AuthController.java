package org.example.campus_performance_ticketing.api;

import net.coobird.thumbnailator.Thumbnails;
import org.example.campus_performance_ticketing.logic.UserService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.LoginRequest;
import org.example.campus_performance_ticketing.logic.dto.LoginResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Value("${user.avatar.upload-dir}")
    private String avatarUploadDir;

    @Value("${user.avatar.base-url}")
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

        // 先从数据库检查是否已有头像路径（需在 UserService 中实现：String getAvatarPathByOpenid(String openid)）
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
                Files.createDirectories(Paths.get(avatarUploadDir));

                String fileName = UUID.randomUUID() + ".jpg";
                File dest = new File(avatarUploadDir + File.separator + fileName);

                try (InputStream in = new URL(request.getAvatar()).openStream()) {
                    Thumbnails.of(in)
                            .size(200, 200)
                            .keepAspectRatio(true)
                            .toFile(dest);
                }

                avatarPath = "/data/avatar/" + fileName;

            } catch (Exception e) {
                return ApiResponse.fail("头像处理失败：" + e.getMessage());
            }
        }

        ApiResponse<LoginResult> response = userService.loginOrRegister(
                request.getOpenid(),
                request.getNickname(),
                avatarPath
        );

        if (response.getData() != null && response.getData().getAvatar() != null) {
            response.getData().setAvatar(avatarBaseUrl + response.getData().getAvatar());
        }

        return response;
    }
}
