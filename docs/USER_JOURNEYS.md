# User Journeys

Two personas drive the whole product:

- **Creator / Teacher** — authors and publishes courses, lectures, and assessments.
- **Learner / Student** — discovers courses, watches lectures, resumes progress, takes
  assessments, and compares scores on leaderboards.

A third implicit actor is the **Platform** (backend + video provider + auth). The journeys
below are the source of truth: modules and APIs are designed to serve them, not the reverse.

---

## Persona map

```mermaid
flowchart LR
    subgraph Creator["👩‍🏫 Creator / Teacher"]
      C1(Create course)
      C2(Add sections & lectures)
      C3(Upload video)
      C4(Build assessment)
      C5(Publish)
      C6(View analytics)
    end
    subgraph Learner["🧑‍🎓 Learner / Student"]
      L1(Discover & enroll)
      L2(Watch lecture)
      L3(Resume progress)
      L4(Take assessment)
      L5(See score & leaderboard)
      L6(Earn completion)
    end
    C5 --> L1
    C3 --> L2
    C4 --> L4
    L4 --> L5
```

---

# Creator journeys

## CJ-1 — Onboard & become a creator

```mermaid
sequenceDiagram
    actor T as Teacher
    participant App as Android App
    participant Auth as Auth (OIDC)
    participant API as Backend API
    T->>App: Open app, tap "Teach"
    App->>Auth: Sign up / sign in
    Auth-->>App: ID + access token (role: learner)
    T->>App: Request creator access
    App->>API: POST /creators (upgrade role)
    API-->>App: role: creator granted
    App-->>T: Creator dashboard
```

**Notes:** everyone signs in as a learner by default; becoming a creator is an explicit
role upgrade. Keep it self-serve for MVP (no manual approval) but gate publishing behind a
completed profile.

## CJ-2 — Author a course (course → sections → lectures)

```mermaid
flowchart TD
    A[New course: title, subject, level, thumbnail] --> B[Add section]
    B --> C[Add lecture to section]
    C --> D{Lecture type}
    D -->|Video| E[Upload video file]
    D -->|Text/notes| F[Rich text body]
    E --> G[Video sent to streaming provider]
    G --> H[Transcode → HLS + signed URL]
    C --> I[Reorder lectures / sections]
    B --> J[Repeat for more sections]
    H --> K[Lecture ready]
    F --> K
    K --> L{Course complete?}
    L -->|No| B
    L -->|Yes| M[Course in DRAFT]
```

Course state machine: `DRAFT → PUBLISHED → (UNPUBLISHED / ARCHIVED)`. Learners only see
`PUBLISHED`.

## CJ-3 — Upload a video lecture (async pipeline)

```mermaid
sequenceDiagram
    actor T as Teacher
    participant App as App
    participant API as Backend API
    participant Vid as Video Provider (Mux/CF Stream)
    T->>App: Select video for lecture
    App->>API: POST /lectures/{id}/upload-url
    API->>Vid: Create direct-upload
    Vid-->>API: one-time upload URL
    API-->>App: upload URL
    App->>Vid: Upload bytes directly (progress bar)
    Vid-->>API: webhook: asset.ready (HLS playback id)
    API->>API: mark lecture READY, store playback id + duration
    Note over T,App: Teacher sees "Processing…" then "Ready"
```

**Why direct-to-provider upload:** the video bytes never touch our backend, so we don't
build storage or transcoding. We only store metadata + the playback id.

## CJ-4 — Build an assessment

```mermaid
flowchart TD
    A[New assessment attached to course/section] --> B[Add question]
    B --> C{Question type}
    C -->|Single choice| D[Options + 1 correct]
    C -->|Multiple choice| E[Options + N correct]
    C -->|True/False| F[Two options]
    D --> G[Set points / weight]
    E --> G
    F --> G
    G --> H[Set pass mark, time limit, attempts allowed]
    H --> I{More questions?}
    I -->|Yes| B
    I -->|No| J[Assessment saved]
```

MVP question types: single-choice, multiple-choice, true/false — all **auto-gradable** so
scoring and leaderboards need no human grading. (Free-text / essay is a later phase and
needs manual or AI grading.)

## CJ-5 — Publish & manage

```mermaid
stateDiagram-v2
    [*] --> Draft
    Draft --> Published: publish (validation passes)
    Published --> Unpublished: hide
    Unpublished --> Published: re-publish
    Published --> Archived: archive
    Archived --> [*]
```

Publish validation: at least one section, one ready lecture, thumbnail present. Editing a
published course creates edits in place (MVP) — versioning is a later concern.

## CJ-6 — View analytics

Enrollment count, per-lecture completion rate, average assessment score, and the leaderboard
for their own assessments. Read-only dashboard backed by aggregate queries.

---

# Learner journeys

## LJ-1 — Discover & enroll

```mermaid
sequenceDiagram
    actor S as Student
    participant App as App
    participant API as Backend API
    S->>App: Browse / search courses
    App->>API: GET /courses?q=&subject=&level=
    API-->>App: published course cards
    S->>App: Open course detail
    App->>API: GET /courses/{id}
    API-->>App: syllabus, lectures (locked preview), assessments
    S->>App: Tap "Enroll"
    App->>API: POST /courses/{id}/enroll
    API-->>App: enrollment created
    App-->>S: Course added to "My Learning"
```

## LJ-2 — Watch a lecture with progress saving  ⭐ core

```mermaid
sequenceDiagram
    actor S as Student
    participant Player as Video Player (Media3)
    participant App as App
    participant API as Backend API
    S->>App: Open lecture
    App->>API: GET /lectures/{id}/playback  (+ last position)
    API-->>App: signed HLS URL, resumeAtSeconds
    App->>Player: load(url), seekTo(resumeAtSeconds)
    loop every ~10s and on pause/background
        Player-->>App: currentPosition
        App->>API: PUT /progress {lectureId, positionSec, watchedSec}
    end
    Player-->>App: playback reached ~95%
    App->>API: PUT /progress {completed: true}
    API-->>App: lecture marked complete, course % updated
```

**Progress design:**
- Progress is throttled (every ~10s + on pause/seek/background) to avoid write storms.
- Writes are **idempotent upserts** keyed by `(learnerId, lectureId)`.
- Client keeps a local copy so it works offline and syncs on reconnect (last-write-wins by
  timestamp). This is what makes "resume on any device" real.
- A lecture is `completed` at ~95% watched; course completion = all lectures complete.

## LJ-3 — Resume where I left off

```mermaid
flowchart LR
    A[Open "My Learning"] --> B[Continue card shows last course/lecture]
    B --> C[Tap Continue]
    C --> D[Player opens at saved position]
    D --> E[Watch continues seamlessly]
```

The home screen leads with a **Continue learning** rail sourced from the most recent
progress record.

## LJ-4 — Take an assessment

```mermaid
sequenceDiagram
    actor S as Student
    participant App as App
    participant API as Backend API
    S->>App: Start assessment
    App->>API: POST /assessments/{id}/attempts
    API-->>App: attempt id, questions (no correct answers), timer
    loop each question
        S->>App: Select answer(s)
        App->>App: Save locally (survive app kill)
    end
    S->>App: Submit
    App->>API: POST /attempts/{id}/submit {answers}
    API->>API: Auto-grade, compute score, update leaderboard
    API-->>App: score, pass/fail, correct answers, rank
    App-->>S: Result screen
```

Guards: enforce attempts-allowed and time limit **server-side** (never trust the client).
Correct answers are only revealed after submit.

## LJ-5 — See my score vs other learners  ⭐ core

```mermaid
sequenceDiagram
    actor S as Student
    participant App as App
    participant API as Backend API
    S->>App: Open leaderboard for assessment/course
    App->>API: GET /assessments/{id}/leaderboard?scope=all&window=all-time
    API-->>App: ranked list (best score per learner) + my rank
    App-->>S: Top N + "You: #rank of total"
```

**Leaderboard rules:**
- Rank by **best score per learner** for that assessment (ties broken by earliest/fastest
  submission).
- A **course leaderboard** aggregates across the course's assessments (sum or average).
- Scopes to design for: `all learners`, `friends/cohort` (later), and time windows
  (`all-time`, `this week`).
- Privacy: display name or opt-in alias; allow a learner to appear anonymously.

## LJ-6 — Complete a course / earn recognition

```mermaid
flowchart LR
    A[All lectures complete] --> C{Assessments passed?}
    B[Pass-mark met on required assessments] --> C
    C -->|Yes| D[Course marked complete]
    D --> E[Completion badge / certificate]
    D --> F[Final rank on course leaderboard]
```

MVP: a completion badge + shareable summary. Formal certificates are a later phase.

---

## Cross-cutting journeys

| Journey | MVP? | Notes |
| --- | --- | --- |
| Sign up / sign in / sign out | ✅ | OIDC; learner role default, creator upgrade. |
| Edit profile & display name | ✅ | Display name feeds the leaderboard. |
| Offline viewing / progress sync | ⚠️ Partial | Progress syncs offline in MVP; full lecture download is a later phase. |
| Notifications (new course, result) | ❌ Later | Push via FCM. |
| Ratings & reviews | ❌ Later | Course quality signal. |
| Payments / paid courses | ❌ Later | Out of scope for MVP; keep the domain open to it. |
| Reporting / moderation | ❌ Later | Needed before public UGC at scale. |

---

## What these journeys pin down for the design

1. **Progress is a first-class aggregate** keyed by `(learner, lecture)` with idempotent
   upserts — it powers Resume, course %, and completion.
2. **Assessments are auto-gradable** in MVP, so **scoring + leaderboards need no human** in
   the loop.
3. **Video never flows through our backend** — direct upload to a provider, we store only
   playback ids and durations.
4. **Grading and attempt limits are server-authoritative** — the client is untrusted.
5. **The API is client-agnostic** — Android, iOS, and Web are three clients over one API.

Next: [ARCHITECTURE.md](ARCHITECTURE.md) turns these into a domain model and modules.
