# Password reset — design

Status: **design only** (AI-006). Not yet implemented. The building blocks it relies on already
exist in the `auth` module; this document specifies the flow, token model, storage, and security
properties so the implementation ticket can proceed and a security review can sign off on the
approach first.

## Goals

- Let a user who forgot their password regain access via a link sent to their registered email.
- **No account enumeration**: the request endpoint must not reveal whether an email is registered.
- Reset tokens are **single-use, short-lived, and unguessable**, and are never stored in plaintext.
- Completing a reset **invalidates existing sessions** (outstanding refresh tokens).
- Reuse the existing password policy and hashing — no new crypto.

## Reused building blocks (already implemented)

| Primitive | Where | Role in reset |
|-----------|-------|---------------|
| `DelegatingPasswordEncoder` (BCrypt default) | `SecurityConfig.passwordEncoder()` | Hash the new password. |
| `User.changePasswordHash(newHash)` | `auth.domain.User` | Set the new password on the aggregate. |
| `User.revokeRefreshTokens()` (bumps `tokenVersion`) | `auth.domain.User` | Kill outstanding refresh tokens on reset. |
| `token_version` check in refresh | `RefreshTokensUseCase` | Enforces the revocation above. |
| Mail transport (Mailpit in dev, SMTP in prod) | `docker-compose.yml`, prod config | Deliver the reset link. |

So the only genuinely new work is: a reset-token table + entity, two endpoints/use cases, an email
template, and two audit actions.

## Flow

```
1. Request
   POST /api/v1/auth/password-reset/request        (public)
   body: { "email": "user@example.com" }
   → 202 Accepted, ALWAYS (whether or not the email exists)

   If the email maps to an ACTIVE user:
     - invalidate that user's prior unused reset tokens
     - generate a 256-bit random token (SecureRandom, base64url)
     - store only its SHA-256 hash with a short expiry
     - email a link: https://<app>/reset-password?token=<opaque-token>

2. Confirm
   POST /api/v1/auth/password-reset/confirm         (public)
   body: { "token": "<opaque-token>", "newPassword": "<min 8 chars>" }

   Server:
     - hash the presented token, look it up
     - reject if not found / expired / already used                → 400 VALIDATION_ERROR
     - user.changePasswordHash(encoder.encode(newPassword))
     - user.revokeRefreshTokens()   // invalidate all existing sessions
     - mark the reset token used
     - emit PASSWORD_RESET_COMPLETED audit event
   → 204 No Content

   The user then logs in again with the new password. Reset does NOT auto-login.
```

## Token model

- **Generation:** 32 bytes from `SecureRandom`, base64url-encoded → the opaque value placed in the
  email link. This is the only place the raw token exists; it is never persisted.
- **Storage:** persist `SHA-256(token)` only. A DB leak therefore does not yield usable tokens.
  (SHA-256 is adequate here because the token is high-entropy and random, unlike a password.)
- **TTL:** 30 minutes (configurable via `app.security.password-reset.ttl`).
- **Single use:** `used_at` is stamped on successful confirm; a used or expired token is rejected.
- **One active token per user:** requesting a new reset invalidates prior unused tokens for that user.

### Proposed migration (V013)

```sql
CREATE TABLE password_reset_tokens (
    id          BINARY(16)  NOT NULL,
    user_id     BINARY(16)  NOT NULL,
    token_hash  CHAR(64)    NOT NULL,   -- hex SHA-256 of the opaque token
    expires_at  DATETIME(6) NOT NULL,
    used_at     DATETIME(6) NULL,
    created_at  DATETIME(6) NOT NULL,
    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_password_reset_user (user_id),
    INDEX idx_password_reset_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
```

## Security properties (for review)

- **No enumeration:** the request endpoint always returns `202`, does the email-lookup work off the
  hot path, and keeps response timing uniform (send-or-skip decided after a constant-cost lookup).
- **Brute force:** tokens are 256-bit random and single-use; guessing is infeasible. Rate-limit the
  request endpoint per email and per source IP to prevent mail flooding / user harassment.
- **Session invalidation:** on successful reset, `revokeRefreshTokens()` bumps `token_version`, so
  every outstanding refresh token is rejected on next use. Short-lived access tokens (default TTL)
  expire naturally; we accept that window by design, consistent with logout.
- **Disabled/pending users:** only `ACTIVE` users receive reset emails; a reset never re-activates a
  disabled account.
- **Password policy:** `newPassword` is validated by the same Bean Validation constraints as
  registration (min length 8), and hashed with the delegating encoder (BCrypt).
- **Auditing:** add `PASSWORD_RESET_REQUESTED` and `PASSWORD_RESET_COMPLETED` to `AuditAction`; the
  request event records only the fact and (if matched) the user id — never the token.
- **Transport:** links use HTTPS in prod; tokens are never logged.

## Open questions for security review

1. Reset TTL — 30 min acceptable, or shorter?
2. Rate-limit thresholds and where they live (gateway vs. application filter).
3. Should completing a reset also send a confirmation ("your password was changed") email?
4. Lockout interaction: should repeated failed logins and password reset share a throttle?

## Out of scope for the design ticket

Implementation (endpoints, use cases, entity, migration V013, email template, `AuditAction`
additions) is deferred to a follow-up build ticket once this design is approved.
