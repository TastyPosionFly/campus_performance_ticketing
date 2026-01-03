package org.example.campus_performance_ticketing.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtTokenUtil jwtTokenUtil;

    public JwtAuthInterceptor(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            unauthorized(response, "缺少 Authorization token");
            return false;
        }

        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        try {
            Claims claims = jwtTokenUtil.parseToken(token);

            // 不拦截封禁用户，只把状态放入 request
            Integer status = claims.get("status", Integer.class);

            // 放入 request，Controller 可直接使用
            request.setAttribute("userId", claims.get("userId", Long.class));
            request.setAttribute("openid", claims.getSubject());
            request.setAttribute("role", claims.get("role", String.class));
            request.setAttribute("status", status);

        } catch (ExpiredJwtException e) {
            unauthorized(response, "token 已过期，请重新登录");
            return false;
        } catch (JwtException e) {
            unauthorized(response, "无效的 token");
            return false;
        }

        return true;
    }

    private void unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
