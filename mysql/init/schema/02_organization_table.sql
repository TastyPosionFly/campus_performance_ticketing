CREATE TABLE organization (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '组织主键ID',

    name VARCHAR(100) NOT NULL COMMENT '组织/团队名称',
    description VARCHAR(255) COMMENT '组织简介',
    avatar VARCHAR(255) COMMENT '组织照片',

    leader_user_id BIGINT COMMENT '组织负责人用户ID',

    status TINYINT DEFAULT 1 COMMENT '组织状态：1-正常 2-停用',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '组织创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='演出组织 / 活动主办方表';
