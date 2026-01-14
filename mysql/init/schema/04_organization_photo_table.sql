CREATE TABLE organization_album (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '照片主键ID',
    organization_id BIGINT NOT NULL COMMENT '组织ID',
    photo_url VARCHAR(255) NOT NULL COMMENT '照片URL',
    uploader_id BIGINT NOT NULL COMMENT '上传者用户ID',
    upload_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
    description VARCHAR(255) COMMENT '照片描述',
    FOREIGN KEY (organization_id) REFERENCES organization_info(id),
    FOREIGN KEY (uploader_id) REFERENCES user_info(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='组织相册表';