CREATE TABLE application (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请主键ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人',
    application_type VARCHAR(40) NOT NULL COMMENT '申请类型：CREATE_ORG/JOIN_ORG/CHANGE_LEADER/EDIT_MEMBER/QUIT_ORG/EDIT_ALBUM/DISSOLVE_ORG等',
    target_id BIGINT COMMENT '目标对象ID，根据申请类型指向组织、成员、相册等',
    extra_data JSON COMMENT '其他参数，灵活扩展',
    status TINYINT DEFAULT 1 COMMENT '申请状态：1-待审核 2-通过 3-拒绝 4-撤销',
    apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    approve_time DATETIME COMMENT '审批时间',
    approver_id BIGINT COMMENT '审批人',
    FOREIGN KEY(applicant_id) REFERENCES user_info(id)
    -- target_id 可关联不同表，自行约束
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一申请审批表';