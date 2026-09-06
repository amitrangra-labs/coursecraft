# Roadmap

Milestone-based, each phase independently demoable. Journeys referenced by their IDs from
[USER_JOURNEYS.md](USER_JOURNEYS.md).

## Phase 0 — Foundations (repo & skeleton)

- [x] Backend skeleton: hexagonal layout, `Application`, 3 config classes, health endpoint.
- [x] PostgreSQL + `schema.sql`, `JdbcClient` wiring (`app_user` baseline).
- [x] JWT verify against Supabase JWKS; `learner` role default + creator upgrade; JIT provisioning.
- [~] Android app skeleton: Compose home screen + backend health check. (Supabase sign-in +
      navigation still to wire.)
- [x] CI: build + test for backend and Android.
- [x] **Signed universal release APK** produced by CI and attached to a **GitHub Release**,
      with published SHA-256 + signing-cert fingerprints (release keystore in Actions
      secrets). This is the MVP distribution channel — see [DISTRIBUTION.md](DISTRIBUTION.md).

**Demo:** download the APK from a GitHub Release, sideload it on a real phone, sign in, and
hit an authenticated backend endpoint.

## Phase 1 — Creator authoring (video pipeline)

- [ ] Course/section/lecture CRUD (CJ-2).
- [ ] Video provider integration: direct-upload URL + `asset.ready` webhook (CJ-3).
- [ ] Creator studio screens: create course, add lecture, upload with progress, publish (CJ-5).

**Demo:** a teacher creates and publishes a course with a playable video lecture.

## Phase 2 — Learner core: watch + resume  ⭐

- [ ] Discovery + enroll (LJ-1).
- [ ] Media3 player with signed HLS playback (LJ-2).
- [ ] Progress upsert API + throttled client writes + Room offline queue + WorkManager sync.
- [ ] "Continue learning" rail; resume-at-position (LJ-3).

**Demo:** watch a lecture, kill the app, reopen → resumes at the exact position.

## Phase 3 — Assessments + leaderboards  ⭐

- [ ] Assessment authoring: single/multiple/true-false questions (CJ-4).
- [ ] Attempt lifecycle with server-side grading, attempt limits, timer (LJ-4).
- [ ] Result screen; best-per-learner leaderboard, assessment + course scope (LJ-5).
- [ ] Course completion + badge (LJ-6).

**Demo:** two learners take the same assessment and see their ranks on a shared leaderboard.

## Phase 3.5 — Live lectures  (MVP)

- [ ] `LiveSession` lifecycle: schedule → live → ended/canceled (CJ-7).
- [ ] YouTube Live integration: create broadcast (or paste-a-link fallback), embed player.
- [ ] "Went live" push + reminders; learner attend screen with countdown/live/replay (LJ-7).
- [ ] Auto-archive the recording into a normal video lecture (reuses LJ-2 replay + progress).

**Demo:** a teacher goes live, learners join and chat, and the recording appears as a replay
lecture afterward.

## Phase 4 — Polish & MVP hardening

- [ ] Creator analytics dashboard (CJ-6).
- [ ] Profile + display name / anonymous leaderboard option.
- [ ] Error/empty/loading states, accessibility, offline messaging.
- [ ] Observability: metrics + structured logs; basic load test on leaderboard/progress.

**MVP done** = Phases 0–4 on Android, **delivered as a sideloadable signed APK** (no store) —
and **live lectures (Phase 3.5) are in scope for the MVP**, not a later add-on. The only
live-related items deferred past MVP are native in-app Q&A and two-way WebRTC seminars. Store
publishing (Google Play / App Store) and the web client are post-MVP.

## Phase 5+ — Beyond MVP

- iOS (SwiftUI) client over the same API.
- Web (React) client.
- Full offline lecture download; push notifications (FCM); ratings & reviews;
  free-text/AI-graded questions; certificates; payments; cohorts/friends leaderboards;
  moderation.
- Interactive two-way live (WebRTC seminars via LiveKit/Jitsi) and native in-app live Q&A —
  when the budget or nonprofit credits can cover real-time infra.

## Suggested build order rationale

Authoring (Phase 1) comes before learner features because you need real courses and videos to
test watching, progress, and assessments. The two ⭐ phases (2 and 3) are the product's
differentiators — **resume-anywhere progress** and **competitive scoring** — so they get
dedicated phases rather than being folded into a generic "learner" milestone.
