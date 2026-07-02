# Database

## Ownership

The schema is owned by **Flyway**. Hibernate runs with `ddl-auto: validate` and never creates or
alters tables. Migrations live in `src/main/resources/db/migration` and are applied in order.

## Conventions

- **UUIDs:** stored as `BINARY(16)` (see `UuidUtils`), consistently across all tables.
- **Money:** `DECIMAL(19,2)`. Never floating point.
- **Timestamps:** UTC; Hibernate `jdbc.time_zone=UTC`.
- Foreign keys and indexes for common lookups are declared in the migration that creates the table.

## Migration set (target)

| Version | File                                      | Introduced in |
|---------|-------------------------------------------|---------------|
| V001    | `V001__create_users.sql`                  | Phase 2       |
| V002    | `V002__create_bankruptcy_applications.sql`| Phase 2       |
| V003    | `V003__create_application_status_history.sql` | Phase 2   |
| V004    | `V004__create_questionnaire_definitions.sql`  | Phase 3   |
| V005    | `V005__create_questionnaire_answers.sql`  | Phase 3       |
| V006    | `V006__create_rule_evaluations.sql`       | Phase 3       |
| V007    | `V007__create_reviews.sql`                | Phase 3       |
| V008    | `V008__create_document_metadata.sql`      | Phase 3       |
| V009    | `V009__create_audit_events.sql`           | Phase 3       |

Migrations are added alongside the module whose entities map them, so `validate` always has a matching
schema. Integration tests run the full migration set against a real MySQL via Testcontainers.
