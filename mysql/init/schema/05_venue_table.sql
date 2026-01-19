-- ----------------------------
-- 场地基础信息表
-- ----------------------------
CREATE TABLE `venues` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` varchar(100) NOT NULL COMMENT '场地名称',
    `description` text COMMENT '场地描述/介绍',
    `address` varchar(255) NOT NULL COMMENT '详细地址',
    `cover_image` varchar(255) DEFAULT NULL COMMENT '封面图片URL',

    -- 详情轮播图列表 (JSON Array)
    -- 存入格式：["https://.../img1.jpg", "https://.../img2.jpg"]
    -- 或者带简单描述：[{"url":"...", "desc":"舞台"}, {"url":"...", "desc":"侧门"}]
    `photo_list` json DEFAULT NULL COMMENT '场地详情轮播图(JSON数组)',

    `capacity` int(10) unsigned DEFAULT '0' COMMENT '容纳人数',
    `type` tinyint(3) unsigned DEFAULT '1' COMMENT '场地类型(1:会议室, 2:剧场等)',

    -- 设备信息 JSON
    `equipment_info` json DEFAULT NULL COMMENT '设备配置(JSON): {"wifi":true, "sound":"JBL"}',

    `status` tinyint(3) unsigned DEFAULT '1' COMMENT '场地状态(1:正常, 0:维护, 2:停用)',

    -- 关联 user_info
    `manager_id` bigint(20) DEFAULT NULL COMMENT '场地管理员ID (user_info.id)',
    `created_by` bigint(20) DEFAULT NULL COMMENT '创建人ID (user_info.id)',

    `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
    `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` datetime DEFAULT NULL,

    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    CONSTRAINT `fk_venues_manager` FOREIGN KEY (`manager_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='场地基础信息表';