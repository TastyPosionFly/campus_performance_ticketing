CREATE TABLE user_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键ID',

    openid VARCHAR(64) NOT NULL COMMENT '微信 openid，用户唯一标识',
    nickname VARCHAR(50) COMMENT '用户昵称',
    avatar VARCHAR(255) COMMENT '用户头像URL',

    user_identity TINYINT DEFAULT 1 COMMENT '用户身份类型：1-学生 2-学校职工 3-校外人员',

    student_no VARCHAR(30) COMMENT '学生学号（仅学生身份有效）',
    major VARCHAR(100) COMMENT '学生专业（仅学生身份有效）',
    college VARCHAR(100) COMMENT '学院/学校名称（可为外校）',

    phone VARCHAR(20) COMMENT '手机号',

    role VARCHAR(20) DEFAULT 'USER' COMMENT '系统角色：USER / ORGANIZER / VENUE_ADMIN / ADMIN',

    status TINYINT DEFAULT 1 COMMENT '账号状态：1-正常 2-封禁',

    last_login_time DATETIME COMMENT '最后登录时间（超过30天需重新登录）',

    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '账户创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '账户更新时间',

    UNIQUE KEY uk_openid (openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户基础信息表';
