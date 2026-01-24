-- 使用数据库
USE campus_ticket;

-- 1. 删除
SOURCE /docker-entrypoint-initdb.d/schema/00_delete_exist_table.sql;

-- 2. 建表
SOURCE /docker-entrypoint-initdb.d/schema/01_user_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/02_organization_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/03_user_organization_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/04_organization_photo_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/05_venue_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/06_venue_opening_hours_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/07_venue_block_day_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/08_performance_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/09_performance_session_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/10_performance_staff_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/15_application_table.sql;

-- 3. 初始化数据
SOURCE /docker-entrypoint-initdb.d/data_dev/init_data.sql;
