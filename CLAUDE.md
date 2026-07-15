# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Backend for a document-preparation platform for Russian personal-bankruptcy cases (`ru.adiaphora`).
Built as a **modular monolith**: one Spring Boot 4.1 / Java 21 application, one MySQL schema, one
deployable artifact, with module boundaries enforced by tests rather than convention.

> Questionnaire content and legal rules are **placeholders** pending lawyer review — don't treat
> business logic in `rules`/`questionnaire` as authoritative without flagging that.

## Commands
 
```bash
# One-time setup
cp .env.example .env
mvn -N wrapper:wrapper          # generates ./mvnw / ./mvnw.cmd (not committed as a binary)

# Full stack (MySQL, MinIO, Mailpit, backend, placeholder frontend), all with health checks
docker compose up --build
docker compose --profile dev-tools up --build   # + Adminer on :8081

# App only, against a MySQL you provide (local profile, seed accounts enabled)
./mvnw spring-boot:run

# Full test suite: unit + integration (Testcontainers/real MySQL) + security + architecture
./mvnw clean verify

# Single test class / method
./mvnw test -Dtest=RuleEngineTest
./mvnw test -Dtest=RuleEngineTest#someMethodName

# Integration tests only need Docker running for Testcontainers (class names end in IntegrationTest)
./mvnw test -Dtest=*IntegrationTest
```

Colima note: Docker Engine 29's socket must be pointed to explicitly for Testcontainers — see
[docs/local-development.md](docs/local-development.md) for the `~/.testcontainers.properties` /
`-DargLine="-Dapi.version=1.44"` workaround. Not needed with Docker Desktop or on CI (GitHub-hosted
runners, plain `./mvnw -B verify`).

Swagger UI (`local` profile only, disabled in `prod`): http://localhost:8080/swagger-ui.html

## Architecture

Feature modules live under `ru.adiaphora.platform`: `auth`, `application` (the bankruptcy case
lifecycle — not the Spring app itself), `questionnaire`, `rules`, `review`, `document`, `audit`, plus
`common` for shared technical building blocks.

Every business module has identical internal layering:

| Layer            | Responsibility                                                             |
|------------------|----------------------------------------------------------------------------|
| `api`            | Public interfaces + DTOs other modules may use. No JPA entities exposed.    |
| `application`    | Use cases, command/query handlers, transactions, orchestration, mapping.   |
| `domain`         | Aggregates, value objects, domain services/events, repository interfaces.  |
| `infrastructure` | REST controllers, JPA entities, Spring Data repositories, adapters, config. |

**Communication rules (mechanically enforced, not just convention):**
- A module may depend only on another module's **`api`** package — never reach into another module's
  `infrastructure` or repositories.
- Side effects that cross module boundaries (especially audit) flow through **Spring application
  events**, not direct calls. `audit` subscribes to domain events from every other module.
- `common` is a declared shared module; it must never depend on a business module.
- Enforced by `ModularityTest` (Spring Modulith boundary verification + module docs generation) and
  `LayeredArchitectureTest` (ArchUnit: domain doesn't depend on infrastructure, controllers don't call
  repositories, etc.) — both run as part of `./mvnw verify`. If you break a boundary, one of these
  tests fails, not a code review comment.

Module dependency direction: `application` depends on `questionnaire`, `rules`, `review`, `document`.
All modules emit events to `audit`.

**Cross-cutting concerns, all in `common`:**
- Errors: a single `@RestControllerAdvice` (`GlobalExceptionHandler`) maps exceptions to the `ApiError`
  contract; DB/framework messages are never exposed to clients. Stable codes live in `ErrorCode`.
- Correlation id: `CorrelationIdFilter` binds `X-Correlation-Id` to MDC for every request; it's echoed
  in the response header and in every `ApiError`.
- Security: stateless, role-based URL rules configured in `common.security.SecurityConfig`; the
  `auth` module contributes the JWT token filter via the `HttpSecurityCustomizer` extension point so
  `common` itself stays independent of `auth`. Per-resource ownership checks (e.g. "this application
  belongs to this user") live in the owning module's application service, *not* in the URL matrix —
  see [docs/security.md](docs/security.md) for the URL authorization table.
- Time: inject the UTC `Clock` bean (`ClockConfig`); never call `Instant.now()` directly in domain code.

**Database:** schema is owned by **Flyway** (`src/main/resources/db/migration`); Hibernate runs with
`ddl-auto: validate` and never creates/alters tables. Add a migration in the same change as any new
entity. Conventions: UUIDs as `BINARY(16)` (`UuidUtils`), money as `DECIMAL(19,2)` (never floating
point), timestamps UTC.

**API conventions:** base path `/api/v1`, JSON/camelCase, separate request/response DTOs (JPA entities
are never serialized), Bean Validation on request bodies, paginated collection endpoints
(`PageResponse` envelope), every endpoint documented via OpenAPI. See [docs/api.md](docs/api.md) for
the error shape and endpoint list.

Roles: `USER`, `OPERATOR`, `LAWYER`, `ADMIN`, `AUDITOR`.

Local seed accounts (one per role, `local` profile only, never `test`/`prod`) are documented in
[docs/local-development.md](docs/local-development.md).

## Further reading

- [docs/architecture.md](docs/architecture.md) — module layering and communication rules in more depth
- [docs/modules.md](docs/modules.md) — per-module status/phase
- [docs/security.md](docs/security.md) — URL authorization matrix, token/password handling
- [docs/database.md](docs/database.md) — migration list and DB conventions
- [docs/api.md](docs/api.md) — error format, pagination envelope, endpoint list
- [docs/local-development.md](docs/local-development.md) — Testcontainers/Colima setup, seed accounts