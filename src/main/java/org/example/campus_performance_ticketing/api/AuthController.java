package org.example.campus_performance_ticketing.api;

import org.example.campus_performance_ticketing.logic.UserService;
import org.example.campus_performance_ticketing.logic.dto.ApiResponse;
import org.example.campus_performance_ticketing.logic.dto.user.LoginRequest;
import org.example.campus_performance_ticketing.logic.dto.user.LoginResult;
import org.example.campus_performance_ticketing.util.AvatarUrlUtil;
import org.example.campus_performance_ticketing.util.FileUtil;
import org.example.campus_performance_ticketing.util.WeChatHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final WeChatHelper weChatHelper;

    @Value("${user.avatar.upload-dir}")
    private String avatarUploadDir;

    @Value("${file.base.url}")
    private String avatarBaseUrl;

    @Value("${wechat.appid}")
    private String wechatAppid;

    @Value("${wechat.secret}")
    private String wechatSecret;

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(UserService userService, WeChatHelper weChatHelper) {
        this.userService = userService;
        this.weChatHelper = weChatHelper;
    }

    /**
     * 登录 / 注册（微信小程序） - 不需要 token
     * 使用 WeChatHelper 验证微信用户的真实性
     */
    @PostMapping("/login")
    public ApiResponse<LoginResult> loginOrRegister(
            @RequestBody LoginRequest request
    ) {
        // ========== 第一步：验证微信登录凭证 code ==========
        if (request.getCode() == null || request.getCode().isBlank()) {
            logger.warn("登录请求缺少微信登录凭证 code");
            return ApiResponse.fail("缺少微信登录凭证，请重新登录");
        }

        // ========== 第二步：使用 WeChatHelper 验证 openid 真实性 ==========
        WeChatHelper.ValidationResult validationResult = weChatHelper.validateOpenid(
                request.getOpenid(),
                request.getCode(),
                wechatAppid,
                wechatSecret
        );

        // 验证失败
        if (!validationResult.ok) {
            logger.warn("微信 openid 验证失败: reason={}, providedOpenid={}, code={}",
                    validationResult.reason,
                    request.getOpenid(),
                    request.getCode());
            return ApiResponse.fail("微信身份验证失败：" + validationResult.reason);
        }

        // 验证成功，获取真实的 openid
        String verifiedOpenid = validationResult.resolvedOpenid;
        logger.info("微信 openid 验证成功: reason={}, verifiedOpenid={}",
                validationResult.reason, verifiedOpenid);

        // ========== 第三步：处理用户头像 ==========
        String avatarPath = null;

        // 先从数据库检查是否已有头像路径
        if (verifiedOpenid != null) {
            try {
                avatarPath = userService.getAvatarPathByOpenid(verifiedOpenid);
            } catch (Exception e) {
                logger.error("获取头像路径失败，openid={}", verifiedOpenid, e);
                // 不阻断流程，继续执行
            }
        }

        // 如果数据库没有头像且前端提供了头像 URL，则下载保存
        if (request.getAvatar() != null && request.getAvatar().startsWith("http")
                && (avatarPath == null || avatarPath.isBlank())) {
            try {
                String safeDir = FileUtil.normalizeUploadDir(avatarUploadDir);
                // 使用 FileUtil 保存头像文件，返回文件保存路径
                avatarPath = FileUtil.saveAvatarFromUrl(request.getAvatar(), safeDir, ".jpg");
                logger.info("成功保存用户头像: openid={}, avatarPath={}", verifiedOpenid, avatarPath);
            } catch (Exception e) {
                logger.error("头像处理失败，openid={}, error={}", verifiedOpenid, e.getMessage(), e);
                // 头像处理失败不阻断登录流程
            }
        }

        // ========== 第四步：执行登录或注册 ==========
        ApiResponse<LoginResult> response = userService.loginOrRegister(
                verifiedOpenid,  // 使用验证通过的真实 openid
                request.getNickname(),
                avatarPath
        );

        // ========== 第五步：构造完整的头像 URL 返回给前端 ==========
        if (response.getData() != null) {
            String avatar = response.getData().getAvatar();
            response.getData().setAvatar(AvatarUrlUtil.buildAvatarUrl(avatar, avatarBaseUrl));
        }

        return response;
    }
}