---
name: Release checklist
about: Open one issue per release. The release is NOT ready until every box is checked and both sign-offs are recorded.
title: "Release vX.Y.Z"
labels: release
---

Release commit: `<sha>` · CI run: `<link>`

## 1. Tests & CI

- [ ] CI is green on the release commit — **both** jobs (Backend — build, tests, migrations / Frontend — typecheck, lint, tests, build)
- [ ] No test was skipped, disabled, or weakened to get to green (check the diff since the last release)

## 2. Migrations

- [ ] Every schema change since the last release has a matching `V<n>__*.sql`; no previously applied migration file was edited
- [ ] `FlywayMigrationIntegrationTest` passed in the release CI run
- [ ] A database backup exists from immediately before deploying (runbook → *Backups and restore*), and its restore command is known to work

## 3. Ruleset version

- [ ] If any rule, threshold, message, or route policy changed: `RuleInputs.RULESET_VERSION` was bumped in the same change
- [ ] Rule/threshold changes have recorded lawyer approval, and the `RuleEngineTest` boundary table matches the approved expectations
- [ ] If the ruleset is still `*-PLACEHOLDER`: the release notes state that legal review is pending

## 4. Security

- [ ] New/changed endpoints are in the URL matrix (docs/security.md) and have ownership checks + role/access tests
- [ ] Production runs `SPRING_PROFILES_ACTIVE=prod` — seed/demo data and Swagger UI are off
- [ ] No default/development secrets in the production `.env` (JWT secrets, DB and MinIO passwords)
- [ ] Audit trail spot-checked on the release build: a login and a case view appear in `/api/v1/audit/events`

## 5. Rollback plan (write it BEFORE deploying)

- [ ] Previous image/tag identified: `<tag>` — redeploying it is the application rollback
- [ ] Migration rollback stance stated: migrations are forward-only; undoing one means restoring the pre-release backup and accepting the data-loss window, or fixing forward
- [ ] Ruleset rollback understood: redeploying the previous version restores the old ruleset; historical evaluations are immutable and version-stamped, so they are unaffected either way

## 6. Demo verification

- [ ] Clean install passes on the release commit: fresh clone → `cp .env.example .env` → `docker compose up --build` → all services healthy
- [ ] Seed login works and the 7 demo cases show their expected routes (2× MFC, court, not-recommended, manual review, 2 drafts)
- [ ] Staff flow works: sign in as lawyer/admin, open the review task, record a decision

## 7. Sign-off — release is not ready without BOTH

- [ ] **Owner** approves — @name, date, comment in this issue
- [ ] **QA / tester** approves — @name, date, comment in this issue

Only after both boxes are checked may this issue be closed and the release tagged.
