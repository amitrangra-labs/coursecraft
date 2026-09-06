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
