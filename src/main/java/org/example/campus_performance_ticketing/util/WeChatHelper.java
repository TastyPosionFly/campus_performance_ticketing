package org.example.campus_performance_ticketing.util;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 微信相关辅助类（小程序场景）
 */

@Component
public class WeChatHelper {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final Logger logger = Logger.getLogger(WeChatHelper.class.getName());

    private volatile String cachedAccessToken;
    private volatile long accessTokenExpireAtEpochSec = 0;

    public WeChatHelper() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(rf);
        this.objectMapper = new ObjectMapper();
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
     * - 若不提供 code，则无法验证，返回 failure。
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
            // 未提供 code，无法验证
            return new ValidationResult(false, "未提供 code，无法验证 openid 的真实性", null);
        }
    }

    private String getAccessToken(String appid, String secret) {
        long now = Instant.now().getEpochSecond();
        if (cachedAccessToken != null && now < accessTokenExpireAtEpochSec - 60) { // 提前 60s 过期
            return cachedAccessToken;
        }

        String url = String.format(
                "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s",
                appid, secret
        );

        String resp = restTemplate.getForObject(url, String.class);
        if (resp == null) throw new IllegalStateException("获取 access_token 失败：响应为空");

        try {
            JsonNode node = objectMapper.readTree(resp);
            if (node.has("errcode") && node.get("errcode").asInt() != 0) {
                throw new IllegalStateException("获取 access_token 失败: " + resp);
            }
            String token = node.get("access_token").asText(null);
            int expiresIn = node.get("expires_in").asInt(0);
            if (token == null || token.isBlank() || expiresIn <= 0) {
                throw new IllegalStateException("获取 access_token 失败: " + resp);
            }

            cachedAccessToken = token;
            accessTokenExpireAtEpochSec = now + expiresIn;
            return token;
        } catch (Exception e) {
            throw new IllegalStateException("解析 access_token 响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取小程序码（不限量）PNG bytes
     * @param scene 最长 32 字符，建议 "id=123"
     * @param page  你的小程序页面路径：pages/performance/detail
     */
    public byte[] getWxaCodeUnlimited(String scene, String page, String appid, String secret) {
        if (scene == null || scene.isBlank()) throw new IllegalArgumentException("scene 不能为空");
        if (scene.length() > 32) throw new IllegalArgumentException("scene 长度不能超过 32");
        if (page == null || page.isBlank()) throw new IllegalArgumentException("page 不能为空");

        String accessToken = getAccessToken(appid, secret);
        String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;

        Map<String, Object> body = new HashMap<>();
        body.put("scene", scene);
        body.put("page", page);
        body.put("check_path", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.POST, entity, byte[].class);
            byte[] bytes = resp.getBody();
            if (bytes == null || bytes.length == 0) {
                throw new IllegalStateException("获取小程序码失败：响应为空");
            }

            // 微信在出错时会返回 JSON（不是图片），这里做一下识别，避免把错误 JSON 当 PNG 返回
            String ct = resp.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            if (ct != null && ct.contains("application/json")) {
                String errJson = new String(bytes, StandardCharsets.UTF_8);
                throw new IllegalStateException("获取小程序码失败: " + errJson);
            }

            return bytes;
        } catch (RestClientException e) {
            throw new IllegalStateException("请求微信小程序码接口失败: " + e.getMessage(), e);
        }
    }
}