# Roadmap

Milestone-based, each phase independently demoable. Journeys referenced by their IDs from
[USER_JOURNEYS.md](USER_JOURNEYS.md).

## Phase 0 — Foundations (repo & skeleton)

- [x] Backend skeleton: hexagonal layout, `Application`, 3 config classes, health endpoint.
- [x] PostgreSQL + `schema.sql`, `JdbcClient` wiring (`app_user` baseline).
- [x] JWT verify against Supabase JWKS; `learner` role default + creator upgrade; JIT provisioning.
- [x] Android app: Compose + email/password Supabase sign-in → authenticated `/api/me`
      (end-to-end: Supabase JWT → backend JWKS verify → provisioned profile).
- [x] CI: build + test for backend and Android.
- [x] **Signed universal release APK** produced by CI and attached to a **GitHub Release**,
      with published SHA-256 + signing-cert fingerprints (release keystore in Actions
      secrets). This is the MVP distribution channel — see [DISTRIBUTION.md](DISTRIBUTION.md).

**Demo:** download the APK from a GitHub Release, sideload it on a real phone, sign in, and
hit an authenticated backend endpoint.

## Phase 1 — Creator authoring (video pipeline)

- [x] Course/section/lecture CRUD (CJ-2) — backend API + JdbcCourseStore + tests.
- [~] Video: MVP pastes a **YouTube id/URL** (stored + normalized) instead of an upload
      pipeline. Direct-upload URL + `asset.ready` webhook (CJ-3) deferred until a paid/credited
      provider is in play.
- [x] Creator studio screens: become creator, create course, add sections, add video lectures,
      publish (CJ-5). Android, CI-green.

**Demo:** a teacher creates and publishes a course with a playable video lecture. ✅

## Phase 2 — Learner core: watch + resume  ⭐

- [x] Discovery + enroll (LJ-1) — catalog, course detail, enroll/unenroll, and a "My Learning"
      list of enrolled courses.
- [x] Player: YouTube IFrame Player (AndroidYouTubePlayer library) plays the lecture (LJ-2).
      Media3/HLS is for provider-hosted video later.
- [x] Progress upsert API (idempotent, sticky completion) + throttled client saves (~10s) +
      resume-at-position. (Room offline queue / WorkManager sync deferred — online save works.)
- [x] "Continue learning" button (LJ-3); course % complete on the detail screen.

**Demo:** watch a lecture, reopen → resumes at the saved position. ✅ (verify video on a device)

## Phase 3 — Assessments + leaderboards  ⭐

- [x] Assessment authoring: single/multiple/true-false questions (CJ-4).
- [x] Attempt lifecycle with server-side auto-grading (LJ-4). (Attempt limits + timer deferred.)
- [x] Result screen; best-per-learner leaderboard (LJ-5).
- [~] Course completion derived from progress; a badge/certificate is post-MVP (LJ-6).

**Demo:** two learners take the same assessment and see their ranks on a shared leaderboard. ✅

## Phase 3.5 — Live lectures  (MVP)

- [x] `LiveSession` lifecycle: schedule → live → ended/canceled (CJ-7).
- [x] YouTube Live via paste-a-link (id/URL, incl. `/live/`), embedded player (LJ-7).
- [~] Learner attend screen with countdown/live/replay (LJ-7). "Went live" push is post-MVP.
- [~] Recording reuse: the same YouTube id serves the replay once ENDED (no re-archive step).

**Demo:** a teacher schedules a session, goes live, learners join; after End, the recording
plays from the same entry. ✅

## Phase 4 — Polish & MVP hardening

- [x] Creator analytics dashboard (CJ-6) — enrollments + per-assessment attempts & avg score.
- [x] Editable display name (feeds the leaderboard). Anonymous-leaderboard option is post-MVP.
- [~] Error/empty/loading states present across screens; deeper accessibility + offline is ongoing.
- [ ] Observability: metrics + structured logs; load test — post-MVP.

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
