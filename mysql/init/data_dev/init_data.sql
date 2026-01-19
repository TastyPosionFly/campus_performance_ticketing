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
INSERT INTO organization_info (name, description, avatar_url, leader_id, status, create_time, update_time)
VALUES
   ('计算机协会', '热爱编程和算法的社团组织', '/data/org-avatar/monalisa-200x200.jpg', 1, 1, NOW(), NOW()),
   ('乐队之家', '面向全校音乐爱好者的乐团', '/data/org-avatar/monalisa-200x200.jpg',2, 1, NOW(), NOW()),
   ('读书沙龙', '定期举行主题读书会的文学社团', '/data/org-avatar/monalisa-200x200.jpg',3, 1, NOW(), NOW());

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
    (1, '/data/org_album/monalisa-500x500.jpg', 1, NOW(), '2026春季编程比赛合影'),
    (2, '/data/org_album/monalisa-500x500.jpg', 2, NOW(), '2026乐队排练现场'),
    (3, '/data/org_album/monalisa-500x500.jpg', 3, NOW(), '读书沙龙活动留影');

-- =========================
-- 6. 插入场地数据 (venues)
-- 注意：manager_id=4 (David, VENUE_ADMIN), created_by=1 (Alice, SUPER_ADMIN)
-- =========================
INSERT INTO `venues` (`name`, `description`, `address`, `cover_image`, `photo_list`, `capacity`, `type`, `equipment_info`, `status`, `manager_id`, `created_by`, `created_at`, `updated_at`)
VALUES
    (
        '主校区大礼堂',
        '适合举办大型晚会、讲座和毕业典礼，拥有专业舞台和后台休息室。',
        '主校区 A栋 101',
        '/data/venue/monalisa-500x500.jpg',
        '["/data/venue/monalisa-500x500.jpg", "/data/venue/monalisa-500x500.jpg"]', -- 模拟多张图
        1200,
        2, -- 类型：剧场/礼堂
        '{"sound": "JBL 7.1专业音响", "lighting": "全套舞台灯光", "projector": true, "wifi": true, "mic_count": 8}',
        1, -- 正常
        4, -- David 管理
        1, -- Alice 创建
        NOW(), NOW()
    ),
    (
        '图文信息中心',
        '配备先进投影设备和舒适座椅，适合举办中小型会议和培训。',
        '主校区 图书馆 305',
        '/data/venue/monalisa-500x500.jpg',
        '["/data/venue/monalisa-500x500.jpg"]',
        50,
        1, -- 类型：会议室
        '{"sound": "普通会议音箱", "lighting": "普通照明", "projector": true, "wifi": true, "whiteboard": true}',
        1, -- 正常
        4, -- David 管理
        1, -- Alice 创建
        NOW(), NOW()
    ),
    (
        '北欧草坪',
        '宽敞的户外草坪，适合举办音乐节、户外电影放映和社交活动。',
        '北校区 中心草坪',
        '/data/venue/monalisa-500x500.jpg',
        NULL, -- 无详情图
        3000,
        3, -- 类型：户外场地
        '{"sound": "需申请移动音箱", "lighting": "无", "power_supply": "220V/380V接口"}',
        0, -- 维护中 (草坪养护)
        4,
        1,
        NOW(), NOW()
    );

-- =========================
-- 7. 插入场地开放时间 (venue_opening_hours)
-- 假设 ID: 1=大礼堂, 2=小礼堂, 3=音乐堂
-- =========================

-- 7.1 大礼堂：周一到周五晚上开放，周末全天开放
INSERT INTO `venue_opening_hours` (`venue_id`, `day_of_week`, `open_time`, `close_time`, `is_closed`) VALUES
    (1, 1, '18:00:00', '22:00:00', 0), -- 周一晚上
    (1, 2, '18:00:00', '22:00:00', 0), -- 周二晚上
    (1, 3, '18:00:00', '22:00:00', 0), -- 周三晚上
    (1, 4, '18:00:00', '22:00:00', 0), -- 周四晚上
    (1, 5, '18:00:00', '22:00:00', 0), -- 周五晚上
    (1, 6, '09:00:00', '22:00:00', 0), -- 周六全天
    (1, 7, '09:00:00', '22:00:00', 0); -- 周日全天

-- 7.2 小礼堂：周一到周五全天开放，周末休息
INSERT INTO `venue_opening_hours` (`venue_id`, `day_of_week`, `open_time`, `close_time`, `is_closed`) VALUES
    (2, 1, '08:00:00', '21:00:00', 0),
    (2, 2, '08:00:00', '21:00:00', 0),
    (2, 3, '08:00:00', '21:00:00', 0),
    (2, 4, '08:00:00', '21:00:00', 0),
    (2, 5, '08:00:00', '21:00:00', 0),
    (2, 6, '00:00:00', '00:00:00', 1), -- 周六休息
    (2, 7, '00:00:00', '00:00:00', 1); -- 周日休息

-- 7.3 北欧草坪：周一到周四开放，周五到周日延长开放时间
INSERT INTO `venue_opening_hours` (`venue_id`, `day_of_week`, `open_time`, `close_time`, `is_closed`) VALUES
    (3, 1, '10:00:00', '20:00:00', 0),
    (3, 2, '10:00:00', '20:00:00', 0),
    (3, 3, '10:00:00', '20:00:00', 0),
    (3, 4, '10:00:00', '20:00:00', 0),
    (3, 5, '10:00:00', '22:00:00', 0),
    (3, 6, '10:00:00', '22:00:00', 0),
    (3, 7, '10:00:00', '22:00:00', 0);

-- =========================
-- 8. 插入场地特殊屏蔽日期 (venue_blocked_days)
-- =========================
INSERT INTO `venue_blocked_days` (`venue_id`, `blocked_date`, `reason`, `created_by`, `created_at`) VALUES
    (1, DATE_ADD(CURDATE(), INTERVAL 5 DAY), '全校电路检修', 4, NOW()), -- 5天后大礼堂停电检修
    (1, DATE_ADD(CURDATE(), INTERVAL 20 DAY), '学校官方占用-校庆彩排', 4, NOW()), -- 20天后校庆占用
    (2, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '会议室投影仪维修', 4, NOW()); -- 明天会议室修投影

