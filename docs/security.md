# Security

## Model

Stateless API authentication with short-lived access tokens and refresh tokens (JWT). No server-side
session. Spring Security enforces coarse URL/role rules; application services enforce per-resource
ownership.

## URL authorization matrix

| Pattern                     | Access                                   |
|-----------------------------|------------------------------------------|
| `/api/v1/auth/**`           | public                                   |
| `POST /api/v1/eligibility/estimate` | public (anonymous estimate; persists nothing) |
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

Password reset is **designed but not yet implemented** — see [password-reset.md](password-reset.md)
for the token model, flow, and security properties (pending security review).

Login is hardened against online guessing: the failure message and response timing are uniform
whether or not the email exists (a fixed dummy hash is compared when there is no real user), and
`LoginAttemptTracker` locks an email after 5 consecutive failures for 15 minutes. The lockout is
in-memory/per-instance for now; a distributed version and per-IP throttling are follow-ups.

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

## Profile selection (fail closed)

There is **no default Spring profile**. A profile must be selected explicitly via
`SPRING_PROFILES_ACTIVE` — `prod` for real deployments, `local` for a developer machine. With no
profile active there are no secret defaults, so the app fails fast at startup rather than silently
booting with the committed development JWT secrets and seeded accounts. The `./mvnw spring-boot:run`
dev command opts into `local` through the `spring-boot-maven-plugin` configuration; the built jar /
container image does **not**, so forgetting to set the profile in production is a startup failure,
not an insecure boot.

## Correlation id is not trusted

The inbound `X-Correlation-Id` header is attacker-controlled. `CorrelationIdFilter` accepts it only
when it matches a strict pattern (`[A-Za-z0-9._-]`, length ≤ 64, kept in sync with the
`audit_events.correlation_id` column); otherwise a fresh id is generated. This prevents log forging /
response-header injection and stops an over-length id from failing the audit insert and rolling back
the audited operation.
