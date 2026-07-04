# API

The canonical, hand-authored contract lives in [`openapi.yaml`](../openapi.yaml) (OpenAPI 3.0, v0.1 —
auth, applications, questionnaire/answers, evaluation, reviews, audit). The running app also serves a
springdoc-generated spec at `/v3/api-docs` and Swagger UI (local profile).

## Conventions

- Base path `/api/v1`; JSON only; camelCase properties.
- UUID identifiers; ISO-8601 UTC timestamps.
- Separate request and response DTOs; JPA entities are never serialised.
- Bean Validation on request bodies.
- Collection endpoints are paginated.
- Every endpoint is documented via OpenAPI (Swagger, local profile).

## Error format

Every failure returns this shape (`fieldErrors` omitted when empty):

```json
{
  "timestamp": "2026-07-01T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/applications",
  "correlationId": "0f0e...-uuid",
  "fieldErrors": [
    { "field": "debtAmount", "message": "must be greater than or equal to zero" }
  ]
}
```

`code` is one of the stable values in `ErrorCode`. The `correlationId` matches the
`X-Correlation-Id` response header and the server logs.

## Pagination envelope

```json
{ "items": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
```

## Endpoints (target)

Implemented per module across phases. See the ticket for the authoritative list.

| Area          | Endpoints |
|---------------|-----------|
| Auth          | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me` |
| Applications  | `POST/GET /applications`, `GET /applications/{id}`, `POST .../submit`, `POST .../cancel`, `GET .../status-history` |
| Questionnaire | `GET /questionnaires/current`, `GET/PUT/POST /applications/{id}/questionnaire...` |
| Rules         | `POST /applications/{id}/evaluate`, `GET /applications/{id}/evaluations/latest` |
| Review        | `GET /reviews`, `GET /reviews/{id}`, `POST .../assign|request-information|approve|reject` |
| Documents     | `POST/GET /applications/{id}/documents`, `GET /documents/{id}`, `GET /documents/{id}/download` |

Concrete request/response examples are added with each module (Phase 2+).
