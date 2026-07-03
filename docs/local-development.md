# Local development

## One-time setup

```bash
cp .env.example .env
mvn -N wrapper:wrapper   # generates ./mvnw and ./mvnw.cmd
```

## Run everything with Docker

```bash
docker compose up --build
# with the optional Adminer DB UI:
docker compose --profile dev-tools up --build
```

Core services (all with health checks): `mysql`, `minio` (+ one-shot `createbuckets`), `mailpit`,
`backend`, `frontend`.

- Frontend: <http://localhost:3000> (placeholder container; proxies `/api` to the backend)
- API: <http://localhost:8080>
- Swagger UI: <http://localhost:8080/swagger-ui.html>
- Health: <http://localhost:8080/actuator/health>
- MinIO console: <http://localhost:9001> (S3 API on :9000)
- Mailpit: <http://localhost:8025>
- Adminer: <http://localhost:8081> (dev-tools profile)

## Run the app against your own MySQL

```bash
./mvnw spring-boot:run
```

Uses the `local` profile by default (see `application-local.yml`).

## Tests

```bash
./mvnw clean verify
```

Integration tests need Docker running (Testcontainers starts MySQL automatically).

## Seed accounts (local profile only)

Synthetic accounts are created on startup when `adiaphora.seed.enabled=true` (default in `local`,
always off in `test`/`prod`). Introduced in Phase 2:

| Role     | Email (synthetic)          |
|----------|----------------------------|
| USER     | user@example.test          |
| OPERATOR | operator@example.test      |
| LAWYER   | lawyer@example.test        |
| ADMIN    | admin@example.test         |
| AUDITOR  | auditor@example.test       |

Passwords are set in the seed runner and printed to the log on startup. **No real personal data is
used anywhere.**
