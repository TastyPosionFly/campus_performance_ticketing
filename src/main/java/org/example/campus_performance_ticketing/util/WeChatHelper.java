package org.example.campus_performance_ticketing.util;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.logging.Logger;

/**
 * 微信相关辅助类（小程序场景）
 */

@Component
public class WeChatHelper {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Logger logger = Logger.getLogger(WeChatHelper.class.getName());

    public WeChatHelper() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(rf);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 简单格式校验：小程序 openid 通常为 28 字符，可根据需要调整正则
     */
    public boolean isValidOpenidFormat(String openid) {
        if (openid == null) return false;
        return openid.matches("^[a-zA-Z0-9_-]{20,32}$");
    }

    /**
     * 调用微信 jscode2session 接口，用 code 换取 openid（小程序场景）
     * 成功返回 openid，失败返回 null
     */
    public String verifyOpenidByJsCode(String code, String appid, String secret) {
        try {
            if (code == null || code.isEmpty() || appid == null || secret == null) return null;
            String url = String.format(
                "https://api.weixin.qq.com/sns/jscode2session?appid=%s&secret=%s&js_code=%s&grant_type=authorization_code",
                appid, secret, code);
            String resp = restTemplate.getForObject(url, String.class);
            if (resp == null) return null;
            JsonNode node = objectMapper.readTree(resp);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                logger.warning("jscode2session 接口返回错误: " + resp);
                return null;
            }
            if (node.has("openid")) {
                return node.get("openid").asText();
            }
            return null;
        } catch (Exception e) {
            logger.warning("调用 jscode2session 接口异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 验证结果封装
     */
    public static class ValidationResult {
        public final boolean ok;
        public final String reason;
        public final String resolvedOpenid; // 来自微信的 openid（若有）

        public ValidationResult(boolean ok, String reason, String resolvedOpenid) {
            this.ok = ok;
            this.reason = reason;
            this.resolvedOpenid = resolvedOpenid;
        }
    }

    /**
     * 聚合验证入口（小程序场景）
     * - 如果提供 code，则以 code 换回的 openid 作为可信来源；
     *   若前端同时提供 openid，会做比对并返回比对结果。
     * - 若不提供 code，仅做格式校验（不可信），返回 failure 建议前端提供 code。
     * 关于 code（微信小程序登录时的临时登录凭证）的简短说明：
     * - code 是小程序客户端调用 wx.login() 后由微信服务器返回的临时凭证（一次性、短时效）。
     * - 前端会在用户授权或需要与后端建立会话时调用 wx.login() 获取 code 并发送到后端。
     * - 后端使用 appid 和 secret 调用 /sns/jscode2session 接口，用 code 换取 openid 和 session_key。
     * - code 不应在客户端长期保存，也不要把 secret 放到前端；后端应当把通过 code 换取到的 openid 作为可信来源。
     */
    public ValidationResult validateOpenid(String providedOpenid, String code, String appid, String secret) {
        if (code != null && !code.isEmpty()) {
            String realOpenid = verifyOpenidByJsCode(code, appid, secret);
            if (realOpenid == null) {
                return new ValidationResult(false, "jscode2session 调用失败或未返回 openid", null);
            }
            if (providedOpenid == null || providedOpenid.isEmpty()) {
                return new ValidationResult(true, "通过 code 获取到 openid", realOpenid);
            }
            if (realOpenid.equals(providedOpenid)) {
                return new ValidationResult(true, "前端 openid 与微信换回 openid 匹配", realOpenid);
            } else {
                return new ValidationResult(false, "前端 openid 与微信换回 openid 不匹配", realOpenid);
            }
        } else {
            // 未提供 code，仅做格式校验但无法确认真实性
            if (providedOpenid == null || providedOpenid.isEmpty()) {
                return new ValidationResult(false, "未提供 code 或 openid，无法验证", null);
            }
            if (isValidOpenidFormat(providedOpenid)) {
                return new ValidationResult(false, "仅进行了格式校验，建议前端提供 code 以完成可信校验", providedOpenid);
            } else {
                return new ValidationResult(false, "openid 格式不合法", providedOpenid);
            }
        }
    }


}
