package org.example.campus_performance_ticketing.util;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

public class FileUtil {
    private static final Logger logger = Logger.getLogger(FileUtil.class.getName());

    // 新增：记录当前active profile
    private static String activeProfile = null;

    /**
     * 项目启动时由配置类注册当前active profile（可多次调用，仅首次生效）
     */
    public static synchronized void setActiveProfile(String profile) {
        if (FileUtil.activeProfile == null && profile != null) {
            FileUtil.activeProfile = profile.trim();
            logger.info("FileUtil激活环境: " + FileUtil.activeProfile);
        }
    }

    // 简单规范化：去除前面的"./"或".\"，并保证结尾是"/"
    public static String normalizeUploadDir(String dir) {
        if (dir == null || dir.isEmpty()) return "";
        String trimmed = dir.trim();
        if (trimmed.startsWith("./")) {
            trimmed = trimmed.substring(2);
        } else if (trimmed.startsWith(".\\")) {
            trimmed = trimmed.substring(2);
        }
        return trimmed.endsWith("/") ? trimmed : trimmed + "/";
    }

    /**
     * 保存MultipartFile头像图片到磁盘目录，并返回保存后的绝对路径
     * @param uploadDir 上传保存目录，推荐normalize过
     * @param avatarFile 上传的文件
     * @param extension 可选，若为null自动取文件后缀
     * @return 完整保存路径（含文件名）
     */
    public static String saveAvatar(MultipartFile avatarFile, String uploadDir, String extension) throws IOException {
        try {
        if (avatarFile == null || avatarFile.isEmpty()) return null;
        String ext = extension;
        if (ext == null) ext = getFileExtension(avatarFile.getOriginalFilename());
        String fileName = UUID.randomUUID() + ext;
        Files.createDirectories(Paths.get(uploadDir));
        File dest = new File(uploadDir, fileName);

        Thumbnails.of(avatarFile.getInputStream())
                .size(200, 200)
                .keepAspectRatio(true)
                .toFile(dest);

        return uploadDir + fileName;
        } catch (IOException e) {
            logger.severe("保存头像失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 下载URL头像并保存到本地，返回本地绝对路径
     * @param avatarUrl 头像URL
     * @param uploadDir 上传保存目录
     * @param extension 可选，若为null自动取URL后缀或缺省jpg
     * @return 完整保存路径（含文件名）
     */
    public static String saveAvatarFromUrl(String avatarUrl, String uploadDir, String extension) throws IOException {
        if (avatarUrl == null || !avatarUrl.startsWith("http")) return null;
        String ext = extension;
        if (ext == null) ext = getUrlFileExtension(avatarUrl);
        if (ext == null || ext.isEmpty()) ext = ".jpg";
        String fileName = UUID.randomUUID() + ext;
        Files.createDirectories(Paths.get(uploadDir));
        File dest = new File(uploadDir, fileName);

        Thumbnails.of(new URL(avatarUrl))
                .size(200, 200)
                .keepAspectRatio(true)
                .toFile(dest);

        return uploadDir + fileName;
    }

    public static String getFileExtension(String filename) {
        if (filename == null) return "";
        int index = filename.lastIndexOf('.');
        return index > 0 ? filename.substring(index) : "";
    }

    public static String getUrlFileExtension(String url) {
        if (url == null) return "";
        String path = url.split("\\?")[0].split("#")[0];
        int idx = path.lastIndexOf('.');
        if (idx > 0 && idx > path.lastIndexOf('/')) {
            String ext = path.substring(idx);
            if (ext.matches("\\.(jpg|jpeg|png|gif|bmp|webp)")) {
                return ext;
            }
        }
        return "";
    }

    // 自动判断profile。dev环境用项目相对路径，生产用原始路径
    public static boolean deletePhysicalFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return false;
        }
        String realPath = filePath;
        // 如果是dev环境，并且filePath是相对路径（或以data/等目录开头），则用相对路径
        if ("dev".equalsIgnoreCase(activeProfile) && !new File(filePath).isAbsolute()) {
            realPath = "./" + filePath.replaceFirst("^[/\\\\]", "");
        }
        File file = new File(realPath);
        if (file.exists() && file.isFile()) {
            boolean deleted = file.delete();
            if (deleted) {
                logger.info("物理文件已删除: " + realPath);
            } else {
                logger.warning("物理文件删除失败: " + realPath);
            }
            return deleted;
        } else {
            logger.warning("物理文件不存在: " + realPath);
        }
        return false;
    }

    /**
     * 保存普通图片文件（如场地图片），不强制压缩为头像尺寸
     */
    public static String saveImage(MultipartFile file, String uploadDir) throws IOException {
        if (file == null || file.isEmpty()) return null;
        String ext = getFileExtension(file.getOriginalFilename());
        if (ext.isEmpty()) ext = ".jpg";

        String fileName = UUID.randomUUID() + ext;
        String finalDir = normalizeUploadDir(uploadDir);
        Files.createDirectories(Paths.get(finalDir));
        File dest = new File(finalDir, fileName);

        // 使用 Thumbnailator 进行简单的质量压缩，防止图片过大，但不裁剪
        Thumbnails.of(file.getInputStream())
                .scale(1.0) // 保持原尺寸
                .outputQuality(0.8) // 80% 质量
                .toFile(dest);

        return finalDir + fileName; // 返回相对路径或绝对路径，取决于 uploadDir
    }

    // FileUtil.java 中新增

    /**
     * 下载URL图片并保存到本地（通用版，不强制压缩尺寸）
     * @param imageUrl 图片URL
     * @param uploadDir 上传保存目录
     * @return 完整保存路径（含文件名）
     */
    public static String saveImageFromUrl(String imageUrl, String uploadDir) throws IOException {
        if (imageUrl == null || !imageUrl.startsWith("http")) return null;

        // 1. 获取后缀
        String ext = getUrlFileExtension(imageUrl);
        if (ext == null || ext.isEmpty()) ext = ".jpg";

        // 2. 准备目录和文件名
        String fileName = UUID.randomUUID() + ext;
        String finalDir = normalizeUploadDir(uploadDir); // 确保目录格式正确
        Files.createDirectories(Paths.get(finalDir));
        File dest = new File(finalDir, fileName);

        // 3. 下载并保存 (使用 Thumbnailator 进行简单压缩优化，防止下载超大图撑爆硬盘，但不裁剪)
        Thumbnails.of(new URL(imageUrl))
                .scale(1.0) // 保持原尺寸
                .outputQuality(0.8) // 80% 质量
                .toFile(dest);

        return finalDir + fileName;
    }
}
