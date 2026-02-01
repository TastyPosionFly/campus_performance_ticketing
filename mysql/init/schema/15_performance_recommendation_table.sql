-- ========================================================
-- 演出推荐位配置表 (performance_recommendation)
-- ========================================================
CREATE TABLE IF NOT EXISTS `performance_recommendation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `performance_id` BIGINT NOT NULL COMMENT '关联演出ID',
    `type` TINYINT NOT NULL COMMENT '推荐位置: 1-首页轮播 2-列表置顶',
    `sort_order` INT DEFAULT 0 COMMENT '排序优先级',
    `start_time` DATETIME DEFAULT NULL COMMENT '展示开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '展示结束时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_rec_valid` (`type`, `start_time`, `end_time`),
    CONSTRAINT `fk_rec_perf` FOREIGN KEY (`performance_id`) REFERENCES `performance` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出人工推荐配置表';