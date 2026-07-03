# Modules

| Module          | Responsibility                                                                 | Status   |
|-----------------|--------------------------------------------------------------------------------|----------|
| `auth`          | Registration, login, password hashing, access/refresh tokens, roles, `/me`.    | Phase 2 ✅ |
| `application`   | Bankruptcy case lifecycle, ownership, route, submission, status history.       | Phase 2 ✅ |
| `questionnaire` | Versioned questionnaire, sections, questions, incremental answers, snapshot.   | Phase 3 ✅ |
| `rules`         | Deterministic rule engine, triggered rules, missing info, preliminary route.   | Phase 3 ✅ |
| `review`        | Manual-review tasks, assignment, decisions, documented route overrides.        | Phase 3 ✅ |
| `document`      | Templates, generation requests, storage abstraction, download permissions.     | Phase 3 ✅ |
| `audit`         | Immutable, event-driven audit log; subscribes to domain events.                | Phase 3 ✅ |
| `common`        | Shared errors, security, web, config, persistence helpers. Shared module.      | Phase 1 ✅ |

## Roles

`USER`, `OPERATOR`, `LAWYER`, `ADMIN`, `AUDITOR`.

## Build order

1. **Phase 1 (done):** project foundation, `common`, config, security baseline, modularity + arch
   tests, Docker/Compose, docs.
2. **Phase 2:** `auth` and `application` end-to-end as the exemplar pattern, with migrations and tests.
3. **Phase 3 (done):** `questionnaire`, `rules`, `review`, `document`, `audit` — all modules implemented,
   wired end-to-end (rules → review/status, document generation/download, event-driven audit).

The canonical, per-module responsibilities, endpoints, and enums are specified in the project ticket
and are implemented module by module following the layering in [architecture.md](architecture.md).
