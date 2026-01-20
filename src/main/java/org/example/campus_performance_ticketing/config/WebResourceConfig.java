package org.example.campus_performance_ticketing.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebResourceConfig implements WebMvcConfigurer {

    @Value("${user.avatar.upload-dir}")
    private String userAvatarDir;

    @Value("${org.avatar.upload-dir}")
    private String orgAvatarDir;

    @Value("${org.album.upload-dir}")
    private String orgAlbumDir;

    @Value("${venue.album.upload-dir}")
    private String venueAlbumDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/data/avatar/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(userAvatarDir));

        registry.addResourceHandler("/data/org-avatar/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(orgAvatarDir));

        registry.addResourceHandler("/data/org_album/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(orgAlbumDir));

        registry.addResourceHandler("/data/venue_album/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(venueAlbumDir));
    }

    private String ensureEndsWithSlash(String dir) {
        if (dir == null || dir.isBlank()) return "";
        return dir.endsWith("/") ? dir : dir + "/";
    }
}