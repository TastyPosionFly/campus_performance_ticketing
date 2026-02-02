package org.example.campus_performance_ticketing.logic;

import org.example.campus_performance_ticketing.util.FileUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.logging.Logger;


/**
 * 定时清理临时文件夹服务
 */
@Service
public class TempFileCleanerService {

    private static final Logger logger = Logger.getLogger(TempFileCleanerService.class.getName());

    // 动态注入临时文件夹配置（可多加几个）
    @Value("${performance.post.temp-dir}")
    private String performancePostTempDir;

    @Value("${staff.photo.temp-dir}")
    private String staffPhotoTempDir;

    /**
     * 每天凌晨2点执行一次（可根据实际改为更频繁）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanPerformancePostTempDir() {
        cleanDir(performancePostTempDir);
    }

    @Scheduled(cron = "0 15 2 * * ?")
    public void cleanStaffPhotoTempDir() {
        cleanDir(staffPhotoTempDir);
    }

    /**
     * 清理指定目录下所有文件
     */
    private void cleanDir(String dirPath) {
        String normalizedDir = FileUtil.normalizeUploadDir(dirPath);
        File dir = new File(normalizedDir);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.warning("文件夹不存在，无需清理: " + normalizedDir);
            return;
        }

        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            logger.info("临时文件夹为空，无需清理: " + normalizedDir);
            return;
        }

        int deletedCount = 0;
        for (File file : files) {
            if (file.isFile()) {
                boolean deleted = FileUtil.deletePhysicalFile(file.getPath());
                if (deleted) deletedCount++;
            }
        }
        logger.info("已清理临时文件夹: " + normalizedDir + "，删除文件数量: " + deletedCount);
    }
}