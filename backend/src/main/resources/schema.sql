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

CREATE TABLE IF NOT EXISTS enrollment (
    id          UUID PRIMARY KEY,
    learner_id  UUID NOT NULL REFERENCES app_user (id),
    course_id   UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (learner_id, course_id)
);
CREATE INDEX IF NOT EXISTS idx_enrollment_learner ON enrollment (learner_id);

-- Assessments (Phase 3): auto-gradable questions + leaderboards.
CREATE TABLE IF NOT EXISTS assessment (
    id         UUID PRIMARY KEY,
    course_id  UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    title      TEXT NOT NULL,
    pass_mark  INT NOT NULL DEFAULT 50,   -- percentage 0-100
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_assessment_course ON assessment (course_id);

CREATE TABLE IF NOT EXISTS question (
    id            UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessment (id) ON DELETE CASCADE,
    text          TEXT NOT NULL,
    type          TEXT NOT NULL,          -- SINGLE | MULTIPLE | TRUE_FALSE
    points        INT NOT NULL DEFAULT 1,
    position      INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_question_assessment ON question (assessment_id);

CREATE TABLE IF NOT EXISTS question_option (
    id          UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES question (id) ON DELETE CASCADE,
    text        TEXT NOT NULL,
    correct     BOOLEAN NOT NULL DEFAULT false,
    position    INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_option_question ON question_option (question_id);

CREATE TABLE IF NOT EXISTS attempt (
    id            UUID PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES assessment (id) ON DELETE CASCADE,
    learner_id    UUID NOT NULL REFERENCES app_user (id),
    score         INT NOT NULL,
    max_score     INT NOT NULL,
    submitted_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_attempt_assessment ON attempt (assessment_id);

-- Playback progress (Phase 2): one row per (learner, lecture); idempotent upsert.
CREATE TABLE IF NOT EXISTS progress (
    learner_id   UUID NOT NULL REFERENCES app_user (id),
    lecture_id   UUID NOT NULL REFERENCES lecture (id) ON DELETE CASCADE,
    position_sec INT NOT NULL DEFAULT 0,
    completed    BOOLEAN NOT NULL DEFAULT false,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (learner_id, lecture_id)
);
CREATE INDEX IF NOT EXISTS idx_progress_learner ON progress (learner_id, updated_at DESC);

-- Live lectures (Phase 3.5): scheduled YouTube Live sessions.
CREATE TABLE IF NOT EXISTS live_session (
    id               UUID PRIMARY KEY,
    course_id        UUID NOT NULL REFERENCES course (id) ON DELETE CASCADE,
    title            TEXT NOT NULL,
    youtube_video_id TEXT NOT NULL,
    status           TEXT NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED|LIVE|ENDED|CANCELED
    starts_at        TIMESTAMPTZ NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_live_course ON live_session (course_id);
