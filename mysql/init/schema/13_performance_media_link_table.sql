-- ========================================================
-- 1. 演出媒体外链表 (performance_media_link)
-- ========================================================
CREATE TABLE IF NOT EXISTS `performance_media_link` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `performance_id` BIGINT NOT NULL COMMENT '关联演出ID',

    `type` TINYINT NOT NULL COMMENT '资源类型: 1-录像回放 2-在线直播',
    `platform` TINYINT NOT NULL DEFAULT 1 COMMENT '平台: 1-Bilibili 2-微信视频号 3-其他链接',

    `external_key` VARCHAR(500) NOT NULL COMMENT '完整跳转链接(URL)',

    `title` VARCHAR(100) DEFAULT NULL COMMENT '标题',
    `sort_order` INT DEFAULT 0 COMMENT '排序权重',
    `app_id` VARCHAR(64) DEFAULT NULL COMMENT '目标小程序AppID(可选)',
    `path` VARCHAR(255) DEFAULT NULL COMMENT '目标小程序路径(可选)',

    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_media_pid` (`performance_id`),
    CONSTRAINT `fk_media_perf` FOREIGN KEY (`performance_id`) REFERENCES `performance` (`id`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出媒体外链表';