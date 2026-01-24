CREATE TABLE `performance_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `performance_id` BIGINT NOT NULL COMMENT '关联演出ID',
    `venue_id` BIGINT NOT NULL COMMENT '场地ID',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `ticket_total` INT NOT NULL DEFAULT 0 COMMENT '总票数',
    `ticket_surplus` INT NOT NULL DEFAULT 0 COMMENT '剩余票数',
    PRIMARY KEY (`id`),
    KEY `idx_performance` (`performance_id`),
    -- 用于快速检测排期冲突
    KEY `idx_conflict` (`venue_id`, `start_time`, `end_time`),

    -- 外键约束
    CONSTRAINT `fk_session_performance` FOREIGN KEY (`performance_id`) REFERENCES `performance` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_session_venue` FOREIGN KEY (`venue_id`) REFERENCES `venues` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出场次表';