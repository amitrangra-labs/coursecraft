# CourseCraft web app

A dependency-free, single-file web client ([`index.html`](index.html)) that talks to the same
Render backend + Supabase auth as the Android app. No build step — it's static, so it hosts free.

## Run locally
Just open `index.html` in a browser, or serve the folder:
```bash
cd web && python3 -m http.server 5173   # then open http://localhost:5173
```
(The backend allows any origin via CORS, so local files work too.)

## Host it free — two options

### Option A — GitHub Pages (already wired)
The [`web` workflow](../.github/workflows/web.yml) auto-deploys `web/` to GitHub Pages on every push.
One-time: **repo Settings → Pages → Source: "GitHub Actions"**. Your site then lives at
`https://amitrangra-labs.github.io/coursecraft/`.

### Option B — Cloudflare Pages (unlimited bandwidth)
In the Cloudflare dashboard: **Workers & Pages → Create → Pages → Connect to Git →** pick the repo.
Set **Build command: (none)** and **Build output directory: `web`**. Deploys on every push, free,
with a `*.pages.dev` URL (custom domain optional).

## Config
The backend/Supabase URLs and the (public) Supabase publishable key are constants at the top of
`index.html`. The backend has CORS enabled, so the browser app can call it from any origin.

## Features
Sign in/up, browse & enroll, play video (YouTube embed or self-hosted MP4 via `<video>`), take
assessments, leaderboards, "Live now", and creator authoring (courses, sections, lectures,
assessments, publish, live sessions, analytics).
