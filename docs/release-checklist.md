# Release process

Every release runs through the same checklist — no exceptions, no from-memory releases. The
checklist lives as a GitHub issue template
([.github/ISSUE_TEMPLATE/release.md](../.github/ISSUE_TEMPLATE/release.md)); open one issue per
release ("New issue" → "Release checklist"), work through the boxes in order, and record both
sign-offs as comments on that issue.

**The gate:** a release may not be tagged, announced, or deployed as "ready" until the issue shows
**both** the Owner and the QA/tester sign-off boxes checked, each backed by a comment from that
person. An unchecked box is a blocked release — there is deliberately no override path. If a box
cannot be honestly checked, the release waits.

## What the sections cover, and why

| Section | Protects against |
|---------|------------------|
| Tests & CI | shipping a commit CI never saw green, or "green" achieved by disabling tests |
| Migrations | schema drift, edited history (checksum mismatch on every existing DB), deploying without a restore point |
| Ruleset version | legal-rule changes shipping silently under the old version stamp; unapproved thresholds |
| Security | endpoints outside the URL matrix, prod running with seed data / Swagger / default secrets |
| Rollback plan | discovering during an incident that migrations can't be un-run |
| Demo verification | a release that passes tests but cannot actually be installed and walked through |
| Sign-off | releases marked ready unilaterally |

Operational commands referenced by the checklist (backup/restore, common errors, health checks)
are in the [runbook](runbook.md).

## Dry run record

| Date | Release | Result |
|------|---------|--------|
| 2026-07-08 | v0.1.0-dryrun (`f4b8638`) | All sections executable; every box checkable except sign-off (by design, needs two people). CI green (run on `main`), migrations V001–V013 verified by test, ruleset `ruleset-2026.07-PLACEHOLDER` — placeholder status noted as §3 requires, clean install + demo scenarios verified same day, MySQL backup/restore commands executed against the compose stack. |
