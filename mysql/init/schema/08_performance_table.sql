-- 1. 演出信息主表 (存放业务数据)
CREATE TABLE `performance` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '演出ID',
    `title` VARCHAR(255) NOT NULL COMMENT '演出标题',
    `description` TEXT COMMENT '演出详情/简介',
    `poster_url` VARCHAR(512) COMMENT '海报图片地址',
    `category_id` INT COMMENT '演出类型ID',

    -- 核心：支持个人或组织申请
    `organizer_type` VARCHAR(20) NOT NULL COMMENT '举办方类型：USER (个人), ORGANIZATION (组织)',
    `organizer_id` BIGINT NOT NULL COMMENT '举办方ID (对应 UserInfo.id 或 Organization.id)',

    -- 状态同步：虽然 Application 表有状态，但为了查询性能（如查询所有已上架演出），
    -- 建议在此表保留业务状态。
    `publish_status` TINYINT DEFAULT 0 COMMENT '发布状态: 0-待审批/草稿, 1-已发布(上架), 2-已下架, 3-已结束',

    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_organizer` (`organizer_type`, `organizer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出基础信息表';