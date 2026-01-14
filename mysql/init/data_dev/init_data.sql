-- =========================
-- 初始化测试数据（使用现有图片）
-- =========================

-- 强制会话字符集（关键！）
SET NAMES utf8mb4;

-- 1. 插入用户数据
INSERT INTO user_info (openid, nickname, avatar, user_identity, student_no, major, college, phone, role, status)
VALUES
    ('openid_001', 'Alice', '/data/avatar/monalisa-200x200.jpg', 1, '20230101', '计算机科学', '上海大学', '13800000001', 'SUPER_ADMIN', 1),
    ('openid_002', 'Bob', '/data/avatar/monalisa-200x200.jpg', 1, '20230102', '电子信息', '复旦大学', '13800000002', 'USER', 1),
    ('openid_003', 'Charlie', '/data/avatar/monalisa-200x200.jpg', 2, NULL, NULL, '上海大学', '13800000003', 'USER', 1),
    ('openid_004', 'David', '/data/avatar/monalisa-200x200.jpg', 3, NULL, NULL, '外校', '13800000004', 'VENUE_ADMIN', 1);

-- 2. 插入组织信息  (leader_id对应user_info的id)
INSERT INTO organization_info (name, description, leader_id, status, create_time, update_time) 
VALUES
   ('计算机协会', '热爱编程和算法的社团组织', 1, 1, NOW(), NOW()),
   ('乐队之家', '面向全校音乐爱好者的乐团', 2, 1, NOW(), NOW()),
   ('读书沙龙', '定期举行主题读书会的文学社团', 3, 1, NOW(), NOW());

-- 3. 插入组织成员（MEMBER/LEADER/MANAGER）
-- 三个组织，每个有两名成员，分别承担不同角色
INSERT INTO organization_member (organization_id, user_id, member_role, join_time, status) 
VALUES
    (1, 1, 'LEADER', NOW(), 1),   -- Alice为计协首领
    (1, 2, 'MEMBER', NOW(), 1),   -- Bob加入计协
    (2, 2, 'LEADER', NOW(), 1),   -- Bob为乐队之家首领
    (2, 3, 'MANAGER', NOW(), 1),  -- Charlie为乐队之家管理员
    (3, 3, 'LEADER', NOW(), 1),   -- Charlie为读书沙龙首领
    (3, 4, 'MEMBER', NOW(), 1);   -- David加入读书沙龙

-- 4. 插入统一申请审批记录（常见业务类型为例）
INSERT INTO application (applicant_id, application_type, target_id, extra_data, status, apply_time) 
VALUES
    (2, 'JOIN_ORG', 1, '{"message":"我想参加编程比赛"}', 1, NOW()), -- Bob申请加入计协
    (4, 'JOIN_ORG', 2, '{"reason":"喜欢吉他"}', 1, NOW()),         -- David申请加入乐队之家
    (3, 'QUIT_ORG', 2, '{"reason":"时间不够"}', 3, NOW());           -- Charlie退出乐队之家（已拒绝）

-- 5. 插入组织相册数据（给每个组织加入1张照片）
INSERT INTO organization_album (organization_id, photo_url, uploader_id, upload_time, description) 
VALUES
    (1, '/data/organization_photos/monalisa-500x500.jpg', 1, NOW(), '2026春季编程比赛合影'),
    (2, '/data/organization_photos/monalisa-500x500.jpg', 2, NOW(), '2026乐队排练现场'),
    (3, '/data/organization_photos/monalisa-500x500.jpg', 3, NOW(), '读书沙龙活动留影');



