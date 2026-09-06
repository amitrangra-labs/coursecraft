# CourseCraft backend

Hexagonal Spring Boot service that owns the **server-authoritative** decisions (see
[../docs/STACK.md](../docs/STACK.md)). Phase 0 scope: health, JWT verification against Supabase,
and profile provisioning + creator upgrade (journey CJ-1).

## Layout (strict hexagonal, explicit wiring)

```
com.coursecraft
├── Application.java            # @SpringBootApplication(proxyBeanMethods=false)
├── domain/
│   ├── object/                 # User, Role, VerifiedToken (plain records/enums)
│   ├── service/                # ProfileService (framework-free)
│   └── config/DomainConfig     # wires domain services by hand
├── port/                       # outbound interfaces: UserStore, TokenVerifier
└── adapter/
    ├── in/
    │   ├── endpoint/           # HealthHandler, MeHandler (functional handlers)
    │   ├── auth/RequestAuth    # bearer-auth as a functional filter (no Spring Security)
    │   └── config/InboundConfig# wires handlers + RouterFunctions by hand
    └── out/
        ├── client/             # JdbcUserStore (JdbcClient), NimbusTokenVerifier (JWKS)
        └── config/OutboundConfig# wires adapters by hand
```

Routing is **functional** (`RouterFunction`), not annotated controllers — this keeps the app
free of `@Controller` and sidesteps the Spring 6.2 rule that only `@Controller` types are routed.

## Endpoints

| Method | Path | Auth | Purpose |
| --- | --- | --- | --- |
| GET | `/api/health` | public | Liveness. |
| GET | `/api/me` | bearer | Return caller's profile, provisioning it on first call. |
| POST | `/api/creators` | bearer | Upgrade caller to creator. |

## Build & test (no infra needed)

```bash
cd backend
mvn test        # unit + web-slice tests (routing/auth verified without a DB)
mvn package     # fat jar at target/coursecraft-backend-*.jar
```

## Run locally

Needs a Postgres and Supabase JWT settings. Copy `.env.example`, then:

```bash
docker compose up -d db                 # local Postgres on :5432 (optional convenience)
export $(grep -v '^#' .env | xargs)     # DATABASE_* + SUPABASE_* vars
mvn spring-boot:run
curl localhost:8080/api/health          # {"status":"UP",...}
```

`schema.sql` is applied on startup (idempotent). For real auth, point `SUPABASE_JWKS_URI` /
`SUPABASE_JWT_ISSUER` at your Supabase project; the app verifies RS256/ES256 tokens against the
project JWKS.

## Deploy (Render free tier)

The repo ships a Render Blueprint ([`../render.yaml`](../render.yaml)) + a
[`Dockerfile`](Dockerfile). In Render: **New → Blueprint**, point at this repo, then set these
env vars in the dashboard (they're `sync:false`, so Render prompts and never stores them in git):
`DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `SUPABASE_JWKS_URI`, `SUPABASE_JWT_ISSUER`.
Render injects `PORT` and health-checks `/api/health`. Free instances sleep after ~15 min idle
(slow first request) — a keep-alive ping avoids it. Fly.io is a drop-in alternative later (same
Dockerfile), best paired with a GraalVM native image for its smaller memory.

## Configuration (env)

| Var | Meaning |
| --- | --- |
| `DATABASE_URL` / `DATABASE_USER` / `DATABASE_PASSWORD` | Supabase Postgres connection. |
| `SUPABASE_JWKS_URI` | `https://<ref>.supabase.co/auth/v1/.well-known/jwks.json` |
| `SUPABASE_JWT_ISSUER` | `https://<ref>.supabase.co/auth/v1` |
| `SUPABASE_JWT_ALGORITHMS` | default `RS256,ES256` |
| `PORT` | HTTP port (default 8080). |
