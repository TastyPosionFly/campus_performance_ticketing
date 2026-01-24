-- 3. 演出演职人员表 (存放导演、演员、剧务等)
CREATE TABLE `performance_staff` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `performance_id` BIGINT NOT NULL COMMENT '关联演出ID',

    -- 如果是校内注册用户，关联ID；如果是校外人员，该字段为NULL
    `user_id` BIGINT COMMENT '关联系统用户ID',

    -- 冗余存储姓名，方便展示，也用于存储校外人员姓名
    `staff_name` VARCHAR(64) NOT NULL COMMENT '人员姓名',

    -- 职位/角色，例如：导演、编剧、男主角、灯光师
    `staff_type` VARCHAR(64) NOT NULL COMMENT '职位/角色名称',

    `staff_avatar` VARCHAR(512) COMMENT '演职人员头像/定妆照',

    `introduction` VARCHAR(512) COMMENT '人员简介/角色介绍',

    `sort_order` INT DEFAULT 0 COMMENT '排序权重，数字越小越靠前',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_performance` (`performance_id`),
    KEY `idx_user` (`user_id`),

    -- 外键约束：演出删除，人员名单自动删除
    CONSTRAINT `fk_staff_performance` FOREIGN KEY (`performance_id`) REFERENCES `performance` (`id`) ON DELETE CASCADE,

    -- 外键约束：用户删除，保留记录但 user_id 置空 (SET NULL)，避免历史演出名单丢失名字
    CONSTRAINT `fk_staff_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出演职人员表';