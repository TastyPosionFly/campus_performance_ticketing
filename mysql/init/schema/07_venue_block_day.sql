-- ----------------------------
-- 场地特殊日期屏蔽表
-- ----------------------------
CREATE TABLE `venue_blocked_days` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `venue_id` bigint(20) unsigned NOT NULL,
    `blocked_date` date NOT NULL COMMENT '被屏蔽的日期 (YYYY-MM-DD)',
    `reason` varchar(255) DEFAULT NULL COMMENT '屏蔽原因',
    `created_by` bigint(20) DEFAULT NULL COMMENT '操作人(user_info.id)',
    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_venue_date` (`venue_id`, `blocked_date`),
    CONSTRAINT `fk_blocked_venue` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地特殊日期屏蔽表';