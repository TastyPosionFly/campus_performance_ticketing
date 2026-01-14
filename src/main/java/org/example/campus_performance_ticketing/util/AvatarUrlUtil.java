package org.example.campus_performance_ticketing.util;

/**
 * 头像 URL 构建工具类
 */

public class AvatarUrlUtil {
    public static String buildAvatarUrl(String avatar, String avatarBaseUrl) {
        if (avatar == null) return null;
        if (avatar.startsWith("http://") || avatar.startsWith("https://")) {
            return avatar;
        }
        if (avatarBaseUrl.endsWith("/") && avatar.startsWith("/")) {
            return avatarBaseUrl + avatar.substring(1);
        } else if (!avatarBaseUrl.endsWith("/") && !avatar.startsWith("/")) {
            return avatarBaseUrl + "/" + avatar;
        }
        return avatarBaseUrl + avatar;
    }
}