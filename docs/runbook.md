# Operations runbook

Day-to-day operation of the Adiaphora platform: starting and stopping, configuration, migrations,
backups, and the errors people actually hit. Installation itself is in the [README](../README.md);
developer-workstation specifics (Testcontainers, Colima, seed accounts) are in
[local-development.md](local-development.md).

## Starting and stopping

```bash
# Full stack: MySQL, MinIO, Mailpit, backend, frontend — all with health checks
cp .env.example .env        # first time only
docker compose up --build   # add -d to run detached

# + Adminer DB UI on :8081
docker compose --profile dev-tools up --build

# Stop (keeps data volumes)
docker compose down

# Stop AND WIPE all data (MySQL + MinIO volumes)
docker compose down -v
```

| Service  | Port (override in `.env`)      | Health check                              |
|----------|--------------------------------|-------------------------------------------|
| frontend | `3000`                         | `GET http://localhost:3000/`               |
| backend  | `8080`                         | `GET http://localhost:8080/actuator/health` |
| MySQL    | `3306`                         | `mysqladmin ping` (inside container)       |
| MinIO    | `9000` (console `9001`)        | `GET /minio/health/live`                   |
| Mailpit  | SMTP `1025`, UI `8025`         | built-in                                   |
| Adminer  | `8081` (dev-tools profile)     | —                                          |

Containers start in dependency order and wait for health — if the backend is "waiting", MySQL is
still initialising (first boot takes ~30 s).

**Logs:**

```bash
docker compose logs -f backend        # follow one service
docker compose logs --since 10m       # everything, last 10 minutes
```

Every request carries an `X-Correlation-Id` (echoed in error responses and stored with audit
records) — grep the backend log for it when investigating a reported error.

## Environment variables

All configuration comes from `.env` (copied from [.env.example](../.env.example) — never commit a
real `.env`). Compose passes them to containers; the same names work as plain environment variables
when running the jar directly.

| Variable | Used by | Notes |
|----------|---------|-------|
| `MYSQL_DATABASE` / `MYSQL_USER` / `MYSQL_PASSWORD` / `MYSQL_ROOT_PASSWORD` | MySQL container + backend | Change all four together. |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | backend when run **outside** compose | Points at your own MySQL. |
| `JWT_ACCESS_SECRET` / `JWT_REFRESH_SECRET` | backend | Long random strings (32+ chars). **Must** be replaced for anything non-local; rotating them invalidates all sessions. |
| `DOCUMENT_STORAGE_PATH` | backend (filesystem storage adapter) | Dev only. |
| `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` / `MINIO_BUCKET` | MinIO + backend S3 adapter | Bucket is created automatically on first start. |
| `CORS_ALLOWED_ORIGINS` | backend | Comma-separated frontend origins (`:3000` compose, `:5173` Vite dev). |
| `SPRING_PROFILES_ACTIVE` | backend | `local` enables Swagger UI + seed/demo data; `prod` disables both. **Never run `prod` with seed data expectations.** |
| `*_PORT` | compose port mappings | Override to dodge port clashes; nothing else changes. |

## Database migrations

The schema is owned by **Flyway** (`src/main/resources/db/migration`, `V001…`). Hibernate runs with
`ddl-auto: validate` — it never creates or alters tables. Migrations apply automatically on backend
startup; there is nothing to run by hand.

- **Adding one:** next `V<n>__snake_case_description.sql`, committed in the same change as the
  entity it backs. Integration tests apply the full set against a real MySQL
  (`FlywayMigrationIntegrationTest` asserts every file applied cleanly), so CI fails on a broken
  migration.
- **Never edit an applied migration.** Flyway records a checksum; editing a file that already ran
  produces a *checksum mismatch* at startup on every existing database. Fix forward with a new
  migration.
- **Failed migration recovery (dev):** a migration that dies half-way leaves a `success = 0` row in
  `flyway_schema_history` and the backend will refuse to start. On a development database the
  simplest fix is `docker compose down -v` (wipe) and start again. If the data matters: manually
  undo the partial DDL, delete the failed row from `flyway_schema_history`, fix the migration file
  (it never ran successfully anywhere, so editing is safe *only* in this case), and restart.

## Backups and restore

What holds state: the **MySQL volume** (all business data), the **MinIO bucket** (generated
documents), and your **`.env`** (secrets — losing the JWT secrets logs everyone out; losing DB
credentials is recoverable, losing the database is not).

```bash
# --- MySQL logical backup (safe while running) ---
docker compose exec mysql sh -c \
  'exec mysqldump --single-transaction -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  > backup-$(date +%Y%m%d-%H%M%S).sql

# --- Restore into a running MySQL (replaces current data) ---
docker compose exec -T mysql sh -c \
  'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < backup-20260708-120000.sql

# --- Documents bucket (MinIO -> local directory) ---
docker compose exec minio sh -c \
  'mc alias set local http://localhost:9000 "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null && \
   mc mirror local/adiaphora-documents /tmp/bucket-backup'
docker compose cp minio:/tmp/bucket-backup ./bucket-backup
```

Restores land on the same schema version they were dumped from; after a restore the backend applies
any newer migrations on next startup. Test a restore once before you rely on the backup. (There is
no automated backup schedule yet — for anything beyond a demo, put the `mysqldump` line on a cron.)

## Common errors

| Symptom | Cause | Fix |
|---------|-------|-----|
| `bind: address already in use` / `port is already allocated` on `compose up` | Another process owns 3306/8080/3000/… | Override the `*_PORT` variables in `.env`. |
| `Cannot connect to the Docker daemon` | Docker isn't running | Start Docker Desktop, or `colima start`. |
| `./mvnw: No such file or directory` | Wrapper scripts are generated, not committed | `mvn -N wrapper:wrapper` once (only needed for running Maven directly — the compose build doesn't use it). |
| Backend exits with *FlywayValidateException: checksum mismatch* | An already-applied migration file was edited | Restore the original file; ship the change as a new migration. Dev shortcut: `docker compose down -v`. |
| Backend exits with *Schema-validation: missing table/column* | Entity changed without a migration (ddl-auto is `validate`) | Add the matching `V<n>__….sql`. |
| Backend container stuck "waiting" or unhealthy | MySQL still initialising, or died | `docker compose logs mysql` — first boot takes ~30 s; if it crash-loops, wipe with `down -v`. |
| Browser console: CORS errors from the frontend | Frontend origin not in the allow-list | Add it to `CORS_ALLOWED_ORIGINS` in `.env` and restart the backend. |
| Seed accounts don't exist / login fails with documented accounts | Not running the `local` profile (seeding is off in `test`/`prod`) | Check `SPRING_PROFILES_ACTIVE=local` in `.env`. Accounts are listed in [local-development.md](local-development.md). |
| `mvnw verify` integration tests can't start containers (Colima) | Testcontainers needs the Colima socket + API version pin | One-time setup in [local-development.md](local-development.md); not needed with Docker Desktop or on CI. |
| Swagger UI is 404 | It is deliberately disabled outside the `local` profile | Use the committed [openapi.yaml](../openapi.yaml), or run the `local` profile. |
| API answers `401` with a fresh token | JWT secrets changed since the token was issued (restart with new `.env`) | Log in again; rotating secrets invalidates all sessions by design. |

## Health and monitoring

- `GET /actuator/health` — liveness (used by the container health check).
- `GET /actuator/info` — build info.
- Audit trail: `GET /api/v1/audit/events` (ADMIN / AUDITOR only) — every login, sensitive read,
  edit, status change, evaluation, review decision and document action, with actor + correlation id.
