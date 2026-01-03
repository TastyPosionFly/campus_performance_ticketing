package org.example.campus_performance_ticketing.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration; // 毫秒

    private Key key;

    @PostConstruct
    public void init() {
        // 使用 UTF-8 编码生成 Key，保证跨平台一致性
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 token
     */
    public String generateTokenWithStatus(Long userId, String openid, String role, Integer status) {
        return Jwts.builder()
                .setSubject(openid)                   // openId 作为 token 主体
                .claim("userId", userId)             // 数据库用户ID
                .claim("role", role)                 // 用户角色
                .claim("status", status)             // 用户状态（1 正常 / 0 封禁）
                .setIssuedAt(new Date())             // 签发时间
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析 token
     */
    public Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 校验 token 是否有效（仅检查签名和过期）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token); // 只验证签名和过期
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 从 token 中获取 openId
     */
    public String getOpenIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从 token 中获取 userId
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get("userId");
        return userId != null ? ((Number) userId).longValue() : null;
    }

    /**
     * 从 token 中获取角色
     */
    public String getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return (String) claims.get("role");
    }

    /**
     * 从 token 中获取状态（封禁或正常）
     */
    public Integer getStatusFromToken(String token) {
        Claims claims = parseToken(token);
        Object status = claims.get("status");
        return status != null ? ((Number) status).intValue() : null;
    }
}
