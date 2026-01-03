-- 使用数据库
USE campus_ticket;

-- 1. 删除
SOURCE /docker-entrypoint-initdb.d/schema/00_delete_exist_table.sql;

-- 2. 建表
SOURCE /docker-entrypoint-initdb.d/schema/01_user_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/02_organization_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/03_user_organization_table.sql;
SOURCE /docker-entrypoint-initdb.d/schema/04_organization_photo_table.sql;

-- 3. 初始化数据
SOURCE /docker-entrypoint-initdb.d/data_dev/init_data.sql;
