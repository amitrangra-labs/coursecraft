-- CourseCraft schema. Applied on startup (spring.sql.init.mode=always). Idempotent.
-- Postgres (Supabase). Visible SQL — no JPA-generated DDL.

CREATE TABLE IF NOT EXISTS app_user (
    id           UUID PRIMARY KEY,
    subject      TEXT NOT NULL UNIQUE,   -- provider 'sub' claim; links to Supabase Auth
    email        TEXT,
    display_name TEXT NOT NULL,
    role         TEXT NOT NULL DEFAULT 'LEARNER',  -- LEARNER | CREATOR
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_app_user_subject ON app_user (subject);

-- Courses / sections / lectures (Phase 1). Lectures store only an external video id.
CREATE TABLE IF NOT EXISTS course (
    id          UUID PRIMARY KEY,
    creator_id  UUID NOT NULL REFERENCES app_user (id),
    title       TEXT NOT NULL,
    subject     TEXT,
    level       TEXT,
    status      TEXT NOT NULL DEFAULT 'DRAFT',   -- DRAFT | PUBLISHED | ARCHIVED
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_course_creator ON course (creator_id);
CREATE INDEX IF NOT EXISTS idx_course_status ON course (status);

CREATE TABLE IF NOT EXISTS course_section (
    id          UUID PRIMARY KEY,
    course_id   UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    title       TEXT NOT NULL,
    position    INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_section_course ON course_section (course_id);

CREATE TABLE IF NOT EXISTS lecture (
    id             UUID PRIMARY KEY,
    section_id     UUID NOT NULL REFERENCES course_section (id) ON DELETE CASCADE,
    title          TEXT NOT NULL,
    type           TEXT NOT NULL DEFAULT 'VIDEO',   -- VIDEO | TEXT
    video_provider TEXT,
    video_id       TEXT,
    position       INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_lecture_section ON lecture (section_id);
