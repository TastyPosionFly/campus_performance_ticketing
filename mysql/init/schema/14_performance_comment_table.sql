-- ========================================================
-- 演出评论表 (performance_comment)
-- ========================================================
CREATE TABLE IF NOT EXISTS `performance_comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `performance_id` BIGINT NOT NULL COMMENT '关联演出ID',
    `user_id` BIGINT NOT NULL COMMENT '关联用户ID',

    `content` VARCHAR(1000) NOT NULL COMMENT '评论内容',

    -- 审核状态 (1-正常 0-隐藏)
    `status` TINYINT DEFAULT 1 COMMENT '状态',

    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',

    PRIMARY KEY (`id`),
    KEY `idx_comment_pid` (`performance_id`),
    KEY `idx_comment_uid` (`user_id`),
    CONSTRAINT `fk_comment_perf` FOREIGN KEY (`performance_id`) REFERENCES `performance` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_comment_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出评论表';