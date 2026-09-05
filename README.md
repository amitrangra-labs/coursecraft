# CourseCraft

A multi-platform education platform where **creators/teachers** publish courses of video
lectures and assessments, and **learners/students** watch, resume where they left off,
take assessments, and see how their scores stack up against other learners.

Target platforms, in order: **Android first → iOS → Web.**

> Status: **Planning.** No application code yet. This repo currently holds the development
> plan and the user journeys that drive the design. See [`docs/`](docs/).

## The idea in one paragraph

Teachers create a course, organise it into sections and lessons, upload video lectures,
and attach quizzes/assessments. Students enroll, watch lectures (with playback progress
saved automatically so they can resume on any device), complete assessments, and get a
score that is placed on a per-assessment and per-course leaderboard so they can compare
themselves against peers.

## Planning documents

| Doc | What it covers |
| --- | --- |
| [docs/USER_JOURNEYS.md](docs/USER_JOURNEYS.md) | Every creator & learner journey, with mermaid flow/sequence diagrams. **Start here.** |
| [docs/STACK.md](docs/STACK.md) | The chosen **$0 nonprofit stack** — what we build vs. what Supabase manages, and cost. |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | System architecture, domain model, tech stack, video pipeline, hexagonal backend. |
| [docs/ROADMAP.md](docs/ROADMAP.md) | Phased, milestone-based delivery plan (MVP → v1 → multi-platform). |

## High-level stack (chosen — $0 to run)

Built to run at **$0/month** for a nonprofit. Full detail and cost table in [docs/STACK.md](docs/STACK.md).

- **Android app:** Kotlin + Jetpack Compose, MVVM + unidirectional data flow, Media3/ExoPlayer for video.
- **Our backend:** Spring Boot (Java), **strict hexagonal** — `domain` / `port` / `adapter/in` / `adapter/out`, explicit bean wiring in `@Configuration` classes, `JdbcClient` + `schema.sql` (no JPA magic). Hosted free (Render/Fly.io). Owns grading, leaderboards, publish rules, progress upserts.
- **Supabase (free, managed):** PostgreSQL (our schema), Auth (JWT, `learner`/`creator` roles), and Storage for thumbnails — so we don't operate a database.
- **Video:** **YouTube/Vimeo unlisted embeds** — free hosting + delivery; we store only the video id. (Upgrade path to signed streaming via nonprofit credits if lectures ever need gating.)
- **Later:** iOS (SwiftUI) + Web (React) reuse the same backend API. See ARCHITECTURE for the cross-platform decision.

## Rationale for Android-native first

Android-native (Compose) gives the cleanest offline/progress-sync and video playback story
for the first platform, and keeps the initial surface small. The backend is deliberately
client-agnostic (a plain JSON/REST API), so iOS and Web are additive clients later rather
than rewrites. A cross-platform alternative (Flutter / Compose Multiplatform) is evaluated
in [ARCHITECTURE.md](docs/ARCHITECTURE.md#cross-platform-decision).
