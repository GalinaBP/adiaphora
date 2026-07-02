# Security

## Model

Stateless API authentication with short-lived access tokens and refresh tokens (JWT). No server-side
session. Spring Security enforces coarse URL/role rules; application services enforce per-resource
ownership.

## URL authorization matrix

| Pattern                     | Access                                   |
|-----------------------------|------------------------------------------|
| `/api/v1/auth/**`           | public                                   |
| `/actuator/health`, `/info` | public                                   |
| `/swagger-ui/**`, `/v3/api-docs/**` | development profile only          |
| `/api/v1/reviews/**`        | `OPERATOR`, `LAWYER`, `ADMIN`, `AUDITOR` |
| `/api/v1/audit/**`          | `ADMIN`, `AUDITOR`                        |
| `/api/v1/applications/**`   | authenticated                            |
| everything else             | authenticated                            |

> URL rules are necessary but **not sufficient**. A `USER` may access only their **own** applications;
> that ownership check lives in the application service, not the URL matrix.

## Passwords

Hashed with a delegating encoder (BCrypt by default; Argon2 hashes also verifiable). Plaintext is
never stored. Password hashes are never returned by any endpoint.

## Tokens

- Access token: short TTL (default 15 min).
- Refresh token: longer TTL (default 30 days), exchanged at `/api/v1/auth/refresh`.
- Secrets come from `JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET` env vars. No real secret is committed.
- Refresh tokens must never appear in logs.

## Responses never leak

- password hashes
- refresh tokens
- internal exception traces / framework or DB messages
- JPA entities (controllers return DTOs only)

401 and 403 are returned as the standard `ApiError` JSON (see [api.md](api.md)) by dedicated
`AuthenticationEntryPoint` / `AccessDeniedHandler`.

## Production hardening

- Swagger and `/v3/api-docs` disabled.
- Actuator exposes only `health` and `info`; environment/secrets are not exposed.
- Seed data disabled.
