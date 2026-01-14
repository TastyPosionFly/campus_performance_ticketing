package org.example.campus_performance_ticketing.api;

import net.coobird.thumbnailator.Thumbnails;
import org.example.campus_performance_ticketing.logic.UserService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.model.UserInfo;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Value("${user.avatar.base-url}")
    private String avatarBaseUrl;

    @Value("${user.avatar.upload-dir}")
    private String avatarUploadDir;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public ApiResponse<UserInfo> getCurrentUser(HttpServletRequest request) {
        String openId = (String) request.getAttribute("openid");
        ApiResponse<UserInfo> response = userService.getCurrentUser(openId);
        fillAvatarUrl(response);
        return response;
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
            @RequestParam(required = false) MultipartFile avatarFile,
            @RequestParam(required = false) String avatarUrl,
            @RequestParam(required = false) Integer userIdentity,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String major,
            @RequestParam(required = false) String college,
            @RequestParam(required = false) String phone
    ) {

        // 从 token 获取 openId
        String openId = (String) request.getAttribute("openid");

        String avatarPath = null;

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String ext = getFileExtension(avatarFile.getOriginalFilename());
                String fileName = UUID.randomUUID() + ext;

                Files.createDirectories(Paths.get(avatarUploadDir));
                File dest = new File(avatarUploadDir, fileName);

                Thumbnails.of(avatarFile.getInputStream())
                        .size(200, 200)
                        .keepAspectRatio(true)
                        .toFile(dest);

                avatarPath = "/data/avatar/" + fileName;

            } catch (IOException e) {
                return ApiResponse.fail("头像上传失败：" + e.getMessage());
            }
        } else if (avatarUrl != null && avatarUrl.startsWith("http")) {
            // 如果提供了头像 URL，则下载并保存
            try {
                Files.createDirectories(Paths.get(avatarUploadDir));

                String ext = getUrlFileExtension(avatarUrl);
                if (ext.isEmpty()) ext = ".jpg";
                String fileName = UUID.randomUUID() + ext;
                File dest = new File(avatarUploadDir, fileName);

                Thumbnails.of(new java.net.URL(avatarUrl))
                        .size(200, 200)
                        .keepAspectRatio(true)
                        .toFile(dest);

                avatarPath = "/data/avatar/" + fileName;

            } catch (IOException e) {
                return ApiResponse.fail("头像处理失败：" + e.getMessage());
            }
        }

        // 调用 Service 层用 openId 更新
        ApiResponse<UserInfo> response = userService.updateProfileByOpenId(
                openId, nickname, avatarPath, userIdentity, studentNo, major, college, phone
        );

        fillAvatarUrl(response);
        return response;
    }


    /* ================= 私有工具方法 ================= */

    private void fillAvatarUrl(ApiResponse<UserInfo> response) {
        if (response.getData() != null) {
            String avatar = response.getData().getAvatar();
            response.getData().setAvatar(AvatarUrlUtil.buildAvatarUrl(avatar, avatarBaseUrl));
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int index = filename.lastIndexOf('.');
        return index > 0 ? filename.substring(index) : "";
    }

    /**
     * 获取 URL 中的文件扩展名
     */
    private String getUrlFileExtension(String url) {
        if (url == null) return "";
        // 去掉参数和片段
        String path = url.split("\\?")[0].split("#")[0];
        int idx = path.lastIndexOf('.');
        if (idx > 0 && idx > path.lastIndexOf('/')) {
            String ext = path.substring(idx);
            // 只保留常见的图片扩展名
            if (ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp)")) {
                return ext;
            }
        }
        return "";
    }

}
