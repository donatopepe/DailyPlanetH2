# Security notes — DailyPlanetH2 1.0.x

## Scope and threat model

This document describes application-level controls for a single-instance, server-rendered
work-order portal. In-scope threats: unauthorised account/profile changes (IDOR), privilege
escalation, cross-site request forgery, stolen/fixed session identifiers, comment ownership
bypass, login guessing and browser content injection/framing. Out of scope: host/OS security,
reverse-proxy/TLS configuration, secrets management platform, external penetration testing
and multi-replica distributed rate limiting.

Report vulnerabilities privately to the repository maintainer; do not publish a working
exploit in a public issue.

## Controls currently implemented

### Authentication and credentials

- Passwords use BCrypt (`PasswordEncoder`); no plaintext password is logged.
- Authentication uses Spring Security's `DaoAuthenticationProvider` and
  `PasswordEncoder.matches`.
- Newly created accounts require a password change at first access.
- Self-service password changes derive the account name from the authenticated session and
  compare the submitted name only as a consistency check; a client cannot select another
  user's account by changing a form value.
- Missing/disabled accounts follow the same failed-login path; a dummy BCrypt check reduces
  obvious timing-based account enumeration.
- Login rate limiting is per `(remote address, normalised username)` bucket: 8 failures in
  15 minutes causes a 15-minute cooldown, response `429 Too Many Requests` with `Retry-After`.
  Live smoke verification confirmed attempts 1–7 redirect to login, attempt 8 returns 429
  with `Retry-After: 900` (the integration test also verifies the next blocked request).
  Configure with `LOGIN_RATE_LIMIT_MAX_FAILURES`, `LOGIN_RATE_LIMIT_MAX_ENTRIES`,
  `LOGIN_RATE_LIMIT_WINDOW` (ISO-8601 duration) and `LOGIN_RATE_LIMIT_BLOCK_DURATION`.
  A successful login clears only that pair's bucket. The in-memory map has a maximum of
  10,000 entries and prunes stale entries.

### Authorisation and IDOR

- `/admin/**` requires `ROLE_ADMIN`; `/user/**` requires `ROLE_USER` or `ROLE_ADMIN`.
- Self-service profile update gets the user from `SecurityContext` and database; submitted
  IDs must match. The controller only updates first name, last name and email: username,
  role, disabled state and password are not taken from the self-service DTO.
- Admin account creation validates the role against `USER`/`ADMIN`. Admin editing validates
  the target exists, duplicate usernames and the allowed role set.
- Comment edit views and POSTs verify that the comment belongs to the requested work order
  and that the principal is the author or an administrator.

### CSRF and sessions

- CSRF stays enabled for every state-changing request (including logout).
- Login rotates the session identifier (`changeSessionId`); session cookie is invalidated on
  logout. At most two concurrent sessions are retained per account; a new login is allowed
  and evicts the oldest session.
- Private responses receive `Cache-Control: no-store`.

### Browser response headers

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- Content Security Policy: `default-src 'self'`, `script-src 'self'`, `style-src 'self'`, no inline scripts/styles,
  `object-src 'none'`, `base-uri 'self'`, `frame-ancestors 'none'`.
- HSTS (one year, subdomains): enable only when production is served over HTTPS through a
  trusted TLS-terminating proxy. Do not expose the production app over plain HTTP directly.
- No inline JavaScript or event handlers in templates. Shared browser code is served from
  `/js/app.js`; inline style attributes were removed from templates.

### Logging

- Request logging is metadata-only; request bodies and headers are not logged. Passwords,
  CSRF tokens, cookies and authorization headers must never be added to logs.
- Production should keep application logs at `INFO`; avoid enabling SQL bind-parameter
  logging because entity rows include password hashes and personal data.

## Configuration and deployment requirements

1. Set `DAILYPLANET_ADMIN_PASSWORD`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` from a secret
   store/environment, not from a committed `.env` file. `.env.example` contains placeholders
   only; production must not use the sample values.
2. Terminate TLS at a trusted reverse proxy; restrict access to the app and PostgreSQL ports.
3. If proxy headers are enabled (`forward-headers-strategy: framework`), configure the app
   behind a trusted proxy that overwrites, rather than appends untrusted, forwarding headers.
4. The login limiter reads `remoteAddr` by default. Set `TRUST_FORWARDED_HEADERS=true` only
   when the app is reachable exclusively through a trusted reverse proxy that overwrites
   `X-Forwarded-For`; never expose the app directly with this option enabled.
5. Rotate any credential that was ever used outside local development. The historic seed
   credential was removed from local git history and the public `main` branch, but the old
   commit remains fetchable by SHA until GitHub Support garbage-collects dangling objects;
   see [`github-history-purge.md`](github-history-purge.md). Treat it as compromised.
6. Use one app instance for the in-memory login limiter. For multiple replicas, move the
   rate-limit state to a shared store (for example Redis) or enforce a stricter limit at the
   trusted proxy/WAF.
7. Keep Java base images, PostgreSQL, Gradle plugins and dependencies current; review CI
   dependency alerts before a production deployment.

## Verification

Security regression coverage includes:

- form-driven IDOR tests (profile ID mismatch, attempted role/disabled escalation);
- comment author/admin ownership checks;
- CSRF missing/present checks;
- response header/CSP assertions;
- rate-limit threshold, cooldown, reset, bucket isolation and bounded map tests;
- full form-flow integration tests, role matrix and end-to-end login/logout tests.

Run all checks:

```bash
./gradlew clean test
```

The rate limiter is per-process; it is not a substitute for a WAF, MFA, vulnerability
scanning or an external penetration test.
