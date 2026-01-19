-- ----------------------------
-- 场地常规开放时间表
-- ----------------------------
CREATE TABLE `venue_opening_hours` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `venue_id` bigint(20) unsigned NOT NULL,
    `day_of_week` tinyint(3) unsigned NOT NULL COMMENT '1=周一, 7=周日',
    `open_time` time NOT NULL COMMENT '开始时间',
    `close_time` time NOT NULL COMMENT '结束时间',
    `is_closed` tinyint(1) DEFAULT '0' COMMENT '1:休息, 0:开放',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_venue_day` (`venue_id`, `day_of_week`),
    CONSTRAINT `fk_hours_venue` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地开放时间配置表';