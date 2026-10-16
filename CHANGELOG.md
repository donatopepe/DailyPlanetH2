# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-10-16

### Added

- Public documentation: `README.md` (English), `README.it.md` (Italian), `docs/`
  (architecture, data model, user guide, migration notes), `CONTRIBUTING.md`,
  `.editorconfig`.
- MIT license.
- Seed of the first administrator account driven by environment variables
  (`AdminAccountInitializer`), idempotent and fail-fast in production.
- Mandatory password change at first access (`User.mustChangePassword` plus
  `ForcedPasswordChangeInterceptor`).
- Centralised error handling (`GlobalExceptionHandler`) with 404/409/500 pages.
- Production environment validation (`ProductionEnvironmentValidator`, an
  `EnvironmentPostProcessor`): the `prod` profile refuses to start when `DB_URL`,
  `DB_USERNAME`, `DB_PASSWORD` or `DAILYPLANET_ADMIN_PASSWORD` are missing, listing exactly
  what to set. Health/info actuator endpoints are public for probes.
- Test suite: 55 tests (unit, JPA, view rendering, route/role matrix, end-to-end flow).
- Cross-platform build hardening: UTF-8 for compilation/tests/runtime, `.gitattributes`
  line-ending normalisation, Gradle toolchain auto-provisioning (JDK 17).
- Multi-stage `Dockerfile` (multi-arch base images, non-root user, container healthcheck on
  `/actuator/health`) and `docker-compose.yml` with PostgreSQL 17, named volume and
  healthcheck-based startup order; `.env.example` with placeholders only.
- GitHub Actions workflow: test matrix on Ubuntu, Windows and macOS plus a multi-arch
  (linux/amd64 + linux/arm64) image build.

### Changed

- **Breaking**: migrated to Spring Boot 3.5.16 / Java 17 / Spring Security 6
  (`javax.*` → `jakarta.*`, `WebSecurityConfigurerAdapter` → `SecurityFilterChain`).
- Source layout moved to the standard Gradle/Maven structure
  (`src/main/java/com/pepedonato/DailyPlanetH2/...`).
- CSRF protection enabled (previously disabled): forms must use `th:action`, AJAX calls
  must send the token.
- Passwords: BCrypt hash stored and verified with `PasswordEncoder.matches` (the previous
  implementation re-hashed on load and stored plaintext when a flag was off).
- Category–request association: the category side is now the inverse side
  (`mappedBy = "categorie"`), removing the duplicate join table.
- Comment–request association: `@ManyToOne` instead of `@OneToOne`, so a request can have
  more than one comment.
- `POST /user/doNewCommento/{id}` and `/user/doNewAnswer/{id}` read the text from a request
  parameter instead of binding the entity (the path variable was being bound to the entity
  id, producing an optimistic-lock failure).
- Logging uses SLF4J without sensitive data; all `System.out` calls removed.

### Fixed

- Authentication was impossible (double encoding of the password on every login).
- Development database reused a reserved table name (`user` → `users`).
- Lazy-initialisation errors on collections used by templates
  (`open-in-view: false` + `@EntityGraph` on repositories).
- `Optional.get()` on missing rows produced HTTP 500 instead of a handled error.
- Views `redirectuser`/`redirectaccount` did not exist; replaced by HTTP redirects.
- AJAX fragment endpoints declared `application/json` while returning HTML.

## [0.0.1] - 2021-03-06

### Added

- Initial import: Spring Boot 2 web application with JPA entities, repositories, services
  and controllers for work-order management, comments, categories and accounts.

<!-- I link di confronto/release verranno aggiunti in fase di rilascio (S9), quando i tag esisteranno. -->
