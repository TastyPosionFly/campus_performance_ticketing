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

    @Value("${performance.post.upload-dir}")
    private String posterRealDir;

    @Value("${staff.photo.upload-dir}")
    private String staffRealDir;

    @Value("${ticket.photo.upload-dir}")
    private String ticketUploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/app/data/avatar/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(userAvatarDir));

        registry.addResourceHandler("/app/data/org-avatar/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(orgAvatarDir));

        registry.addResourceHandler("/app/data/org_album/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(orgAlbumDir));

        registry.addResourceHandler("/app/data/venue_album/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(venueAlbumDir));

        registry.addResourceHandler("/app/data/performance_post/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(posterRealDir));

        registry.addResourceHandler("/app/data/staff_photo/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(staffRealDir));

        registry.addResourceHandler("/app/data/ticket_photo/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(ticketUploadDir));

        registry.addResourceHandler("/data/avatar/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(userAvatarDir));

        registry.addResourceHandler("/data/org-avatar/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(orgAvatarDir));

        registry.addResourceHandler("/data/org_album/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(orgAlbumDir));

        registry.addResourceHandler("/data/venue_album/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(venueAlbumDir));

        registry.addResourceHandler("/data/performance_post/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(posterRealDir));

        registry.addResourceHandler("/data/staff_photo/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(staffRealDir));

        registry.addResourceHandler("/data/ticket_photo/**")
                .addResourceLocations("file:" + ensureEndsWithSlash(ticketUploadDir));
    }

    private String ensureEndsWithSlash(String dir) {
        if (dir == null || dir.isBlank()) return "";
        return dir.endsWith("/") ? dir : dir + "/";
    }
}