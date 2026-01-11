-- =========================
-- 初始化测试数据（使用现有图片）
-- =========================

-- 强制会话字符集（关键！）
SET NAMES utf8mb4;

-- 1. 插入用户数据
INSERT INTO user_info (openid, nickname, avatar, user_identity, student_no, major, college, phone, role, status)
VALUES
    ('openid_001', 'Alice', '/data/avatar/monalisa-200x200.jpg', 1, '20230101', '计算机科学', '上海大学', '13800000001', 'USER', 1),
    ('openid_002', 'Bob', '/data/avatar/monalisa-200x200.jpg', 1, '20230102', '电子信息', '复旦大学', '13800000002', 'USER', 1),
    ('openid_003', 'Charlie', '/data/avatar/monalisa-200x200.jpg', 2, NULL, NULL, '上海大学', '13800000003', 'USER', 1),
    ('openid_004', 'David', '/data/avatar/monalisa-200x200.jpg', 3, NULL, NULL, '外校', '13800000004', 'VENUE_ADMIN', 1);

-- 2. 插入组织数据
INSERT INTO organization (name, description, avatar, leader_user_id, status)
VALUES
    ('音乐社', '校园音乐爱好者社团', '/data/organization_photos/monalisa-500x500.jpg', 1, 1),
    ('科技协会', '科技创新与开发', '/data/organization_photos/monalisa-500x500.jpg', 3, 1),
    ('舞蹈团', '校园舞蹈团队', '/data/organization_photos/monalisa-500x500.jpg', 2, 1);

-- 3. 插入用户-组织关联关系
INSERT INTO user_organization (user_id, organization_id, role)
VALUES
    (1, 1, 'LEADER'),
    (2, 1, 'MEMBER'),
    (3, 2, 'LEADER'),
    (4, 2, 'MEMBER'),
    (2, 3, 'LEADER');

-- 4. 插入组织照片
INSERT INTO organization_photo (organization_id, url)
VALUES
    (1, '/data/organization_photos/monalisa-500x500.jpg'),
    (2, '/data/organization_photos/monalisa-500x500.jpg'),
    (3, '/data/organization_photos/monalisa-500x500.jpg');
