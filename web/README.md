# CourseCraft web app

A dependency-free, single-file web client ([`index.html`](index.html)) that talks to the same
Render backend + Supabase auth as the Android app. No build step — it's static, so it hosts free.

## Run locally
Just open `index.html` in a browser, or serve the folder:
```bash
cd web && python3 -m http.server 5173   # then open http://localhost:5173
```
(The backend allows any origin via CORS, so local files work too.)

## Host it free — Cloudflare Pages (in use)

Live at **https://coursecraft.sweeeter.com**, served by Cloudflare Pages (unlimited bandwidth,
free SSL).

To make pushes auto-deploy, the Pages project must be **connected to this Git repo**
(not a Direct Upload project): in the Cloudflare dashboard go to **Workers & Pages → the
`coursecraft` project → Settings → Builds & deployments → Connect to Git**, pick the repo, then set:
- Production branch: `main`
- Build command: *(empty)*
- Build output directory: `web`

After that it rebuilds on every `git push`. For a one-off manual deploy instead:
```bash
npx wrangler pages deploy web --project-name=coursecraft
```

## Config
The backend/Supabase URLs and the (public) Supabase publishable key are constants at the top of
`index.html`. The backend has CORS enabled, so the browser app can call it from any origin.

## Features
Sign in/up, browse & enroll, play video (YouTube embed or self-hosted MP4 via `<video>`), take
assessments, leaderboards, "Live now", and creator authoring (courses, sections, lectures,
assessments, publish, live sessions, analytics).
