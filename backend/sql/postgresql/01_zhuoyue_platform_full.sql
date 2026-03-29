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

-- =========================================================
-- 卷王榜功能新增（2026-03-28）
-- =========================================================

-- mission_stage 新增阶段时间窗口字段
ALTER TABLE mission_stage
  ADD COLUMN IF NOT EXISTS start_time TIMESTAMP,
  ADD COLUMN IF NOT EXISTS deadline   TIMESTAMP;

-- 学生档案表
CREATE TABLE IF NOT EXISTS student_profile (
  id                BIGINT        PRIMARY KEY,
  real_name         VARCHAR(50)   NOT NULL,
  platform          VARCHAR(20)   NOT NULL,
  platform_username VARCHAR(100)  NOT NULL,
  repo_name         VARCHAR(200)  NOT NULL,
  avatar_url        VARCHAR(500),
  display_name      VARCHAR(100),
  active_status     VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
  created_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- 学生阶段进度表
CREATE TABLE IF NOT EXISTS student_stage_progress (
  id               BIGINT    PRIMARY KEY,
  student_id       BIGINT    NOT NULL REFERENCES student_profile(id) ON DELETE CASCADE,
  stage_id         BIGINT    NOT NULL REFERENCES mission_stage(id)   ON DELETE CASCADE,
  is_completed     BOOLEAN   NOT NULL DEFAULT FALSE,
  commit_count     INT       NOT NULL DEFAULT 0,
  last_commit_sha  VARCHAR(80),
  last_commit_time TIMESTAMP,
  synced_at        TIMESTAMP,
  UNIQUE(student_id, stage_id)
);

-- =========================================================
-- 多仓库 + AI 分析功能升级（2026-03-29）
-- =========================================================

-- 1. 删除 student_profile 中的旧 repo_name 字段（全面迁移到 student_repo 表）
ALTER TABLE student_profile DROP COLUMN IF EXISTS repo_name;

-- 2. mission_stage 新增详细任务描述字段（供 AI 分析使用，比 task_title 更详细）
ALTER TABLE mission_stage
  ADD COLUMN IF NOT EXISTS task_description TEXT;

-- 3. 新增学生仓库表（一学生多仓库）
CREATE TABLE IF NOT EXISTS student_repo (
  id          BIGINT        PRIMARY KEY,
  student_id  BIGINT        NOT NULL REFERENCES student_profile(id) ON DELETE CASCADE,
  platform    VARCHAR(20)   NOT NULL,
  repo_name   VARCHAR(200)  NOT NULL,
  repo_type   VARCHAR(20)   NOT NULL DEFAULT 'MANUAL',
  is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
  UNIQUE(student_id, repo_name)
);

-- 4. student_stage_progress 新增 AI 分析字段和人工修改字段
ALTER TABLE student_stage_progress
  ADD COLUMN IF NOT EXISTS repo_id          BIGINT REFERENCES student_repo(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS ai_score         SMALLINT,
  ADD COLUMN IF NOT EXISTS ai_feedback      TEXT,
  ADD COLUMN IF NOT EXISTS ai_analyzed_at   TIMESTAMP,
  ADD COLUMN IF NOT EXISTS manual_score     SMALLINT,
  ADD COLUMN IF NOT EXISTS manual_feedback  TEXT,
  ADD COLUMN IF NOT EXISTS manual_edited_at TIMESTAMP;

-- 新增主分析仓库标记字段（2026-03-29）
ALTER TABLE student_repo
  ADD COLUMN IF NOT EXISTS is_primary BOOLEAN NOT NULL DEFAULT FALSE;
