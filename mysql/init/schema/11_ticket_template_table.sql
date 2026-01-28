-- ==========================================
-- 电子票模板表 (TicketTemplate)
-- 用于存储票面的视觉素材（如背景图）和发布状态
-- ==========================================
CREATE TABLE `ticket_template` (
    `id` bigint NOT NULL AUTO_INCREMENT,

    -- 外键关联：关联到具体的场次
    `session_id` bigint NOT NULL COMMENT '关联场次ID',

    -- 票面素材
    `background_img_url` varchar(255) DEFAULT NULL COMMENT '电子票背景图URL',

    -- 状态控制：
    -- 0: 下架 (前端不显示该样式，或显示默认样式)
    -- 1: 上架 (当前生效的电子票样式)
    `status` int NOT NULL DEFAULT 0 COMMENT '状态: 0-下架 1-上架',

    `created_at` datetime(6) DEFAULT NULL,
    `updated_at` datetime(6) DEFAULT NULL,

    PRIMARY KEY (`id`),
    KEY `idx_template_session` (`session_id`),

    -- 【外键约束】
    -- 当场次被删除时，对应的票面模板级联删除
    CONSTRAINT `fk_template_session`
    FOREIGN KEY (`session_id`) REFERENCES `performance_session` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;