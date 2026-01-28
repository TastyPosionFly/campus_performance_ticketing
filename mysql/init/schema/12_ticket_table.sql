-- ==========================================
-- 用户票据表 (Ticket)
-- 记录用户实际持有的票，以及核销/到场数据
-- ==========================================
CREATE TABLE `ticket` (
    `id` bigint NOT NULL AUTO_INCREMENT,

    -- 核心：唯一核销码
    `ticket_code` varchar(64) NOT NULL COMMENT '唯一核销码',

    -- 归属与来源
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `performance_id` bigint NOT NULL COMMENT '演出ID',
    `session_id` bigint NOT NULL COMMENT '场次ID',

    -- 状态机：
    -- 0: 已预约 (有效，但未入场)
    -- 1: 已核销 (实际已到场，统计人数以此状态为准)
    -- 2: 已取消 (用户主动取消)
    -- 3: 已失效 (演出结束仍未核销)
    `status` int NOT NULL DEFAULT 0 COMMENT '状态: 0-已预约 1-已核销 2-已取消 3-已失效',

    -- 统计数据
    `check_in_time` datetime(6) DEFAULT NULL COMMENT '实际入场时间',
    `check_in_operator_id` bigint DEFAULT NULL COMMENT '检票员ID',

    `created_at` datetime(6) DEFAULT NULL,
    `updated_at` datetime(6) DEFAULT NULL,

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_code` (`ticket_code`),
    KEY `idx_ticket_user` (`user_id`),
    -- 联合索引方便统计某场次的到场人数
    KEY `idx_perf_session_status` (`performance_id`, `session_id`, `status`),

    -- 【外键约束】
    -- 1. 关联用户表 (假设表名为 user_info)
    CONSTRAINT `fk_ticket_user`
    FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

    -- 2. 关联演出主表 (假设表名为 performance)
    CONSTRAINT `fk_ticket_performance`
    FOREIGN KEY (`performance_id`) REFERENCES `performance` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

    -- 3. 关联场次表 (假设表名为 performance_session)
    CONSTRAINT `fk_ticket_session`
    FOREIGN KEY (`session_id`) REFERENCES `performance_session` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;