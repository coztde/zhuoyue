-- =========================================================
-- 卓越班数字化管理平台 - PostgreSQL 初始化/迁移脚本
-- 功能：
-- 1. 新库初始化
-- 2. 旧结构 mission_stage + mission_item 迁移到单表 mission_stage
-- =========================================================

DROP TABLE IF EXISTS student;

CREATE TABLE IF NOT EXISTS admin_user (
                                          id BIGINT PRIMARY KEY,
                                          username VARCHAR(64) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(64),
    active_status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS mission_stage (
                                             id BIGINT PRIMARY KEY,
                                             code VARCHAR(128),
    title VARCHAR(128),
    stage_order INTEGER DEFAULT 0,
    task_title TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

ALTER TABLE mission_stage
    ALTER COLUMN task_title TYPE TEXT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'mission_item'
    ) THEN
        EXECUTE '
            UPDATE mission_stage s
            SET task_title = m.title
            FROM (
                SELECT DISTINCT ON (stage_id)
                       stage_id, title
                FROM mission_item
                ORDER BY stage_id, item_order ASC NULLS LAST, created_at ASC NULLS LAST, id ASC
            ) m
            WHERE s.id = m.stage_id
              AND (s.task_title IS NULL OR BTRIM(s.task_title) = '''')
        ';
END IF;
END $$;

UPDATE mission_stage
SET title = COALESCE(NULLIF(BTRIM(title), ''), CONCAT('阶段 ', stage_order)),
    task_title = COALESCE(NULLIF(BTRIM(task_title), ''), NULLIF(BTRIM(title), ''), CONCAT('阶段 ', stage_order)),
    code = COALESCE(NULLIF(BTRIM(code), ''), CONCAT('course-stage-', stage_order, '-', id));

ALTER TABLE mission_stage
    ALTER COLUMN code SET NOT NULL,
ALTER COLUMN title SET NOT NULL,
    ALTER COLUMN task_title SET NOT NULL;

DROP INDEX IF EXISTS idx_mission_item_stage_id;
DROP TABLE IF EXISTS mission_item;

CREATE UNIQUE INDEX IF NOT EXISTS idx_admin_user_username ON admin_user(username);

DROP INDEX IF EXISTS idx_mission_stage_order;
CREATE UNIQUE INDEX IF NOT EXISTS idx_mission_stage_order ON mission_stage(stage_order);

INSERT INTO admin_user (
    id, username, password_hash, display_name, active_status, created_at, updated_at
) VALUES (
             9001,
             'admin',
             'pbkdf2$65536$UYoTd5IsTWahGc4IWzFE7w==$urDVuJMj450w7oLCqFvLYVM+YsAvcM6rOJPi//6jd5k=',
             '系统管理员',
             'ACTIVE',
             CURRENT_TIMESTAMP,
             CURRENT_TIMESTAMP
         )
    ON CONFLICT (id) DO UPDATE
                            SET username = EXCLUDED.username,
                            password_hash = EXCLUDED.password_hash,
                            display_name = EXCLUDED.display_name,
                            active_status = EXCLUDED.active_status,
                            updated_at = CURRENT_TIMESTAMP;
