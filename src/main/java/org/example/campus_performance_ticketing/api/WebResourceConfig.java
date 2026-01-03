package org.example.campus_performance_ticketing.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 获取服务器中图片网址
 */

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    @Value("${user.avatar.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/data/avatar/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
}
