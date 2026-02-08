package org.example.campus_performance_ticketing.config;

import org.example.campus_performance_ticketing.util.JwtAuthInterceptor;
import org.example.campus_performance_ticketing.util.JwtTokenUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtTokenUtil jwtTokenUtil;

    public WebConfig(JwtTokenUtil jwtTokenUtil) {
        this.jwtTokenUtil = jwtTokenUtil;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new JwtAuthInterceptor(jwtTokenUtil))
                .addPathPatterns("/**") // 拦截所有接口
                .excludePathPatterns(
                        "/api/auth/login",           // 登录注册接口不拦截
                        "/swagger-ui/**",            // swagger文档不拦截
                        "/v3/api-docs/**",            // swagger api-docs 不拦截
                        "/app/data/**",
                        "/data/**"
                );
    }
}
