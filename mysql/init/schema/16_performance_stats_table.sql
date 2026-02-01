-- ========================================================
-- 演出数据聚合统计表 (performance_stats)
-- ========================================================
CREATE TABLE IF NOT EXISTS `performance_stats` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `performance_id` BIGINT NOT NULL COMMENT '关联演出ID',

    `view_count` BIGINT DEFAULT 0 COMMENT '页面浏览量',
    `share_count` BIGINT DEFAULT 0 COMMENT '分享数',
    `comment_count` BIGINT DEFAULT 0 COMMENT '评论总数',

    `ticket_sold_count` INT DEFAULT 0 COMMENT '已预约/售出票数(来自Ticket表)',
    `ticket_check_in_count` INT DEFAULT 0 COMMENT '实际核销/到场人数(来自Ticket表)',

    `hot_score` DOUBLE DEFAULT 0 COMMENT '综合热度分',

    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stats_pid` (`performance_id`),
    KEY `idx_stats_hot` (`hot_score` DESC),
    CONSTRAINT `fk_stats_perf` FOREIGN KEY (`performance_id`) REFERENCES `performance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出数据聚合统计表';