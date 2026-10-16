# DailyPlanetH2

Work-order management portal: open and classify job requests, comment and answer them,
organise categories, manage user accounts and passwords.

*Original project (2021) migrated to a current stack and made publicly distributable:
Spring Boot 3.5, Java 17, Spring Security 6, Thymeleaf, H2/PostgreSQL, Gradle wrapper.*

- License: MIT (see [LICENSE](LICENSE))
- Italian documentation: [README.it.md](README.it.md)
- Implementation plan and status: [TODO.md](TODO.md)

---

## Table of contents

1. [Features](#features)
2. [Technology stack](#technology-stack)
3. [Requirements](#requirements)
4. [Quick start](#quick-start)
5. [Configuration](#configuration)
6. [Database](#database)
7. [Roles and main flows](#roles-and-main-flows)
8. [HTTP routes](#http-routes)
9. [Testing](#testing)
10. [Build and run as a jar](#build-and-run-as-a-jar)
11. [Project structure](#project-structure)
12. [Troubleshooting](#troubleshooting)
13. [Roadmap](#roadmap)
14. [License](#license)

## Features

- **Work orders (job requests)**: create, edit and close a request, with title,
  description, categories and author.
- **Comments and answers**: many comments per request, answers linked to a specific
  comment, all with author and timestamps.
- **Categories**: each request can belong to several categories; categories are looked up
  or created by name.
- **Accounts**: administrator area to list, create, edit and disable accounts, and to
  reset a password.
- **Password security**: BCrypt hashes only (never plaintext, never logged), current
  password required for self-service change, **mandatory password change at first
  access** for accounts created by the seed or by an administrator.
- **Access control**: form login, two roles (`ROLE_USER`, `ROLE_ADMIN`), CSRF protection
  enabled on every form and AJAX call, custom 403 page, per-account/IP login rate limiting
  (8 failures in 15 minutes → 15-minute cooldown).
- **Cybersecurity hardening**: session ID rotation at login, no-store on private pages,
  CSP/anti-framing/nosniff/referrer headers, self-service IDOR prevention and ownership
  checks for comment editing. See [`docs/security.md`](docs/security.md) for threat model,
  controls and limitations.
- **Seed account**: the first administrator is created from an environment variable, never
  from a default committed in the code.
- **Error handling**: centralised `@ControllerAdvice` with 404/409/500 pages and logging
  without sensitive data.

## Technology stack

| Layer | Choice |
|---|---|
| Language | Java 17 (Gradle toolchain, auto-provisioned if missing) |
| Framework | Spring Boot 3.5.16 |
| Web | Spring MVC + Thymeleaf |
| Security | Spring Security 6 (form login, BCrypt, CSRF) |
| Persistence | Spring Data JPA / Hibernate 6 |
| Database | H2 (file, development) · PostgreSQL (production) |
| Build | Gradle 8.14 with committed wrapper |
| Test | JUnit 5, Mockito, MockMvc, `@DataJpaTest`, `@SpringBootTest` |

## Requirements

- **JDK 17 or newer**. The Gradle toolchain resolves and downloads JDK 17 automatically
  when needed (`org.gradle.toolchains.foojay-resolver-convention`), so a local JDK is
  convenient but not mandatory.
- No Gradle installation needed: use the wrapper (`./gradlew`) included in the repository.
- A database: none to install for development (H2 file), PostgreSQL for production.

## Quick start

Commands are identical on Linux, macOS and Windows (use `gradlew.bat` instead of
`./gradlew` in Windows `cmd`/PowerShell).

### Linux / macOS

```bash
git clone https://github.com/donatopepe/DailyPlanetH2.git
cd DailyPlanetH2

# optional: choose the seed administrator password (default in dev profile is "admin")
export DAILYPLANET_ADMIN_PASSWORD='scegli-una-password-robusta'

./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Windows (PowerShell)

```powershell
git clone https://github.com/donatopepe/DailyPlanetH2.git
Set-Location DailyPlanetH2

$env:DAILYPLANET_ADMIN_PASSWORD = 'scegli-una-password-robusta'

.\gradlew.bat bootRun --args='--spring.profiles.active=dev'
```

### Windows (cmd)

```bat
git clone https://github.com/donatopepe/DailyPlanetH2.git
cd DailyPlanetH2
set DAILYPLANET_ADMIN_PASSWORD=scegli-una-password-robusta
gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Then open <http://localhost:8080/login> and sign in with the seed administrator
(`administrator` / the password you set, default `admin` in the dev profile). At the first
access the portal **forces a password change**.

## Configuration

All configuration lives in `src/main/resources/application.yml` and the profile files
`application-dev.yml` / `application-prod.yml`. Every value can be overridden by an
environment variable (12-factor style). Windows equivalent of `export VAR=value` is
`set VAR=value` (cmd) or `$env:VAR = 'value'` (PowerShell).

| Variable | Default | Description |
|---|---|---|
| `SERVER_PORT` | `8080` | HTTP port |
| `DAILYPLANET_ADMIN_USERNAME` | `administrator` | Username of the seeded administrator |
| `DAILYPLANET_ADMIN_EMAIL` | `admin@example.invalid` | E-mail of the seeded administrator |
| `DAILYPLANET_ADMIN_PASSWORD` | `admin` (**dev profile only**) | Password of the seeded administrator. **Mandatory in production**: startup fails without it |
| `DB_URL` | – (required in prod) | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/dailyplanet` |
| `DB_USERNAME` | – (required in prod) | Database user |
| `DB_PASSWORD` | – (required in prod) | Database password |
| `DB_POOL_SIZE` | `10` | HikariCP maximum pool size (prod) |

Profiles:

| Profile | Database | Notes |
|---|---|---|
| `dev` (default) | H2 file `./data/dailyplanet` | H2 console at `/h2-console`, SQL logging off, default admin password documented |
| `prod` | PostgreSQL | No default credentials, H2 console disabled, `forward-headers-strategy: framework` |

## Database

### Development (H2, default)

The development profile uses an H2 **file** database in `./data/dailyplanet`, a path
relative to the working directory, so the same command works on Linux, macOS and Windows.
The schema is created/updated by Hibernate (`ddl-auto: update`).

> **After upgrading from an older checkout**: the category–request association was fixed
> (the inverse side is no longer a duplicate owner), so the obsolete `categoria_domande`
> table is gone. Delete the development database and let it be recreated:
> `rm -rf data` (Linux/macOS) or `rmdir /s /q data` (Windows cmd).

### Production (PostgreSQL)

```bash
export DB_URL='jdbc:postgresql://localhost:5432/dailyplanet'
export DB_USERNAME='dailyplanet'
export DB_PASSWORD='…'
export DAILYPLANET_ADMIN_PASSWORD='…'
java -jar build/libs/DailyPlanetH2-1.0.0.jar --spring.profiles.active=prod
```

The schema is managed by Hibernate (`ddl-auto: update`); versioned migrations
(Flyway/Liquibase) are on the roadmap.

## Roles and main flows

| Role | Can do |
|---|---|
| Anonymous | read `/`, `/about`, `/home`, sign in |
| `ROLE_USER` | everything under `/user/**`: create and edit requests, comment, answer, change own password, edit own account |
| `ROLE_ADMIN` | everything under `/admin/**`: list/create/edit/disable accounts, reset passwords — plus the `/user/**` area |

Seeded administrator flow: first login → forced password change → normal use.
Account created by an administrator: `mustChangePassword` is set, the user changes it at
first login.

## HTTP routes

| Method | Path | Access | Purpose |
|---|---|---|---|
| GET | `/`, `/home`, `/about` | public | landing page |
| GET | `/login`, POST `/login` | public | form login |
| POST | `/logout` | authenticated | logout (CSRF protected) |
| GET | `/403` | public | access denied page |
| GET | `/user` | USER, ADMIN | user area |
| GET | `/user/newdomanda`, POST `/user/doNewDomanda/SaveQuestion` | USER, ADMIN | create a request |
| GET | `/user/domanda/{id}` | USER, ADMIN | request detail with comments/answers |
| POST | `/user/doNewCommento/{id}`, POST `/user/doNewAnswer/{id}` | USER, ADMIN | add a comment / an answer |
| GET | `/user/edituser`, POST `/user/doEditUser` | USER, ADMIN | edit own account |
| GET | `/user/changepw`, POST `/user/doChangepw` | USER, ADMIN | change own password |
| GET | `/user/listdomande[?filters]` | USER, ADMIN | request lists (fragments) |
| GET | `/admin` | ADMIN | administrator area |
| GET | `/admin/account`, `/admin/newaccount`, `/admin/edit/{id}` | ADMIN | account list / create / edit |
| GET | `/admin/changepw/{id}`, POST `/admin/doAdminChangepw` | ADMIN | reset a password |

AJAX note: the fragment endpoints accept `POST` with the CSRF token taken from the
`<meta name="_csrf">` tag rendered by the shared layout (`window.dpCsrf.fetch(...)`).

## Testing

```bash
./gradlew clean test           # Linux / macOS
gradlew.bat clean test         # Windows
```

55 automated tests, split as follows:

| Test class | Scope |
|---|---|
| `DailyPlanetH2ApplicationTests` | application context starts |
| `MyUserDetailsServiceTest` | BCrypt hashing, login, disabled users, password change |
| `AdminAccountInitializerTest` | seed idempotency, fail-fast without password in prod |
| `CategoriaServiceImplTest`, `DomandaServiceImplTest`, `CommentoServiceImplTest` | service logic with mocked repositories |
| `UserRepositoryTest`, `DomandaLavoroRepositoryTest` | JPA mapping, queries, `EntityGraph` loading, many comments per request |
| `ViewRenderingTest` | every Thymeleaf view/fragment renders |
| `RoleAccessMatrixTest` | routes × roles (anonymous → login, USER → 403 on admin, ADMIN → all) |
| `EndToEndFlowTest` | login → create request → comment → read → logout |

HTML report: `build/reports/tests/test/index.html`.

## Build, run as a jar and with Docker

```bash
./gradlew clean build
java -jar build/libs/DailyPlanetH2-1.0.0.jar --spring.profiles.active=dev --server.port=8080
```

Health endpoint (public, no authentication): `GET /actuator/health` → `{"status":"UP"}`.

### Docker

Prerequisites: Docker with the Compose v2 plugin.

```bash
cp .env.example .env      # then edit the values (passwords are mandatory)
docker compose up --build  # app on http://localhost:8080 + PostgreSQL
docker compose down        # add -v to drop the database volume as well
```

What the stack does:

- `db`: PostgreSQL 17 with a named volume (`db-data`) and a `pg_isready` healthcheck;
- `app`: image built from `Dockerfile`, `SPRING_PROFILES_ACTIVE=prod`, database and seed
  credentials injected from `.env`, container healthcheck on `/actuator/health`;
- startup order: the app waits for the database to become healthy;
- no default passwords anywhere: if a variable is missing, Compose stops with an explicit
  message and the application refuses to start (see `ProductionEnvironmentValidator`).

Single image:

```bash
docker build -t dailyplaneth2:1.0.0 .
docker run --rm -e SPRING_PROFILES_ACTIVE=dev -p 8080:8080 dailyplaneth2:1.0.0
```

Multi-architecture image (linux/amd64 + linux/arm64) — requires buildx and QEMU:

```bash
docker buildx build --platform linux/amd64,linux/arm64 -t dailyplaneth2:1.0.0 .
```

The same build runs in CI ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)), together
with the test suite on Linux, macOS and Windows.


## Project structure

```
build.gradle, settings.gradle   Gradle build (Spring Boot plugin, Java 17 toolchain)
gradlew, gradlew.bat, gradle/   Gradle wrapper 8.14
src/main/java/com/pepedonato/DailyPlanetH2/
    config/       Spring Security, Web MVC interceptors, seed initializer
    controller/   Admin, User and public controllers
    error/        access denied handler + centralised exception handler
    model/        JPA entities and form/backing objects
    repository/   Spring Data repositories (with EntityGraph fetching)
    service/      business services
src/main/resources/
    application*.yml         configuration (common, dev, prod)
    templates/               Thymeleaf views (layouts/ + pages + AJAX fragments)
    static/css/app.css       minimal stylesheet, no external assets
src/test/java/...            55 automated tests
docs/                        architecture, data model, user guide, migration notes
TODO.md                      implementation plan, decisions, evidence
```

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `401/403` on a form POST | missing CSRF token: use `th:action` on forms, `window.dpCsrf.fetch` for AJAX |
| Login fails with the seeded administrator | password comes from `DAILYPLANET_ADMIN_PASSWORD`; in dev the default is `admin`, and the first access forces a change |
| Startup fails with `Profilo 'prod': variabili d'ambiente obbligatorie mancanti: DB_URL, DB_USERNAME, DB_PASSWORD, DAILYPLANET_ADMIN_PASSWORD` | expected in the `prod` profile: set every listed variable (no default secrets; the check runs before the context starts) |
| Startup fails with `DAILYPLANET_ADMIN_PASSWORD non impostata` | database configured but no seed password in the `prod` profile: set the variable |
| Strange schema/constraint errors in dev | obsolete H2 database: `rm -rf data` and restart |
| `Could not find or load main class` on Windows | use `gradlew.bat`, and run it from the repository root |
| Page shows "Si è verificato un errore" | check the application log: the exception is logged with a correlation of method and path |

## Roadmap

- Versioned schema migrations (Flyway/Liquibase).
- Screenshots and a short demo video in the README.
- Optional: Keycloak/OIDC login, e-mail notifications on new comments.

## License

MIT © Donato Pepe — see [LICENSE](LICENSE).
