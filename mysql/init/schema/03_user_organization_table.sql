CREATE TABLE organization_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    organization_id BIGINT NOT NULL COMMENT '组织ID',
    user_id BIGINT NOT NULL COMMENT '成员用户ID',
    member_role VARCHAR(20) DEFAULT 'MEMBER' COMMENT '角色：MEMBER/LEADER/MANAGER',
    join_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    status TINYINT DEFAULT 1 COMMENT '状态：1-在组织 2-已退出 3-被踢出',
    FOREIGN KEY (organization_id) REFERENCES organization_info(id),
    FOREIGN KEY (user_id) REFERENCES user_info(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织成员表';
