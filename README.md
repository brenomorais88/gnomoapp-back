# daily-back

Backend for personal/family bill management, designed to evolve into a SaaS-ready product.

## Architecture Summary

The project follows a **modular monolith** with **feature-first + layered architecture**:

- features are organized by business capability (`categories`, `accounts`, `accountoccurrences`, `dashboard`)
- each feature follows `api`, `application`, `domain`, `infrastructure`
- shared cross-cutting code lives under `shared`

Primary reference docs:

- `docs/architecture.md`
- `docs/engineering-rules.md`

Key engineering rules:

- no business logic in routes
- no HTTP concerns in application/domain logic
- repositories only for persistence concerns
- money uses `BigDecimal`
- due/start/end dates use `LocalDate`
- audit timestamps use `Instant`
- identifiers use `UUID`
- database names use `snake_case`

## Feature Coverage

Implemented backend modules:

- Categories CRUD with uniqueness and in-use deletion protection
- Accounts CRUD + activate/deactivate + recurrence-driven occurrence generation
- Account Occurrences querying and operations (mark/unmark paid, amount override)
- Dashboard query endpoints for home summary, day details, category summary, and next 12 months projection
- Daily recurrence maintenance service + scheduler configuration

## Recurrence Generation Overview

Occurrence generation rules:

- `UNIQUE`: single occurrence on `startDate`
- `DAILY`: every day
- `WEEKLY`: same weekday as `startDate`
- `MONTHLY`: same day-of-month as `startDate`, falling back to last day for short months

Generation behavior:

- rolling horizon: next 24 months
- generation runs immediately on account create/update/activate
- account deactivation removes future pending occurrences only
- paid or past occurrences are preserved on recalculation
- generation is idempotent (`account_id + due_date` uniqueness)

## Scheduler Overview

In-process recurrence maintenance scheduler:

- runs at configurable interval (default 24h)
- processes active recurring accounts
- fills only missing future occurrences within rolling 24 months
- does not modify paid occurrences

Current limitation:

- designed for a single backend instance; multi-instance production should externalize scheduling/locking to avoid duplicated triggers.

## Local Setup

### Prerequisites

- Java 17
- Docker + Docker Compose

### Environment configuration

Copy `.env.example` to `.env` and adjust values.

Environment variables:

- `APP_HOST`: internal app bind host (default `0.0.0.0`)
- `APP_PORT`: internal app port in container/app runtime (default `8080`)
- `APP_EXTERNAL_PORT`: host port for API access via Compose (default `8081`)
- `DB_HOST`: database host (`postgres` in Compose)
- `DB_PORT`: database container port (default `5432`)
- `DB_EXTERNAL_PORT`: host port mapped to Postgres (default `5433`)
- `DB_NAME`: database name
- `DB_USER`: database user
- `DB_PASSWORD`: database password
- `DB_SCHEMA`: default schema
- `DB_SSL`: enable SSL in JDBC URL (`true`/`false`)
- `FLYWAY_ENABLED`: enable migrations at startup
- `FLYWAY_LOCATION`: migration location (default `classpath:db/migration`)
- `SEED_ENABLED`: enable default category seeding at startup
- `RECURRENCE_MAINTENANCE_ENABLED`: enable in-process maintenance scheduler
- `RECURRENCE_MAINTENANCE_INTERVAL_HOURS`: scheduler interval in hours

## Running the Project

### Run with Docker Compose (recommended)

```bash
docker compose up -d --build
```

Main endpoints:

- Health: `http://localhost:8081/health`
- Swagger UI: `http://localhost:8081/swagger`

Stop:

```bash
docker compose down
```

### Run app locally with Docker Postgres

```bash
docker compose up -d postgres
./gradlew run
```

## Migrations and Seed Strategy

- Flyway migrations are versioned and immutable under `src/main/resources/db/migration`.
- Seeds are explicit application startup logic (not mixed into versioned schema migrations).
- Default category seed is idempotent and safe to run multiple times.

## Testing and Quality Gates

Useful commands:

- tests: `./gradlew test`
- coverage gate (Kover, min 70%): `./gradlew koverVerify`
- coverage reports: `./gradlew koverXmlReport koverHtmlReport`
- full build: `./gradlew build`

Merge gate requirements:

- tests passing
- coverage >= 70%
- successful `./gradlew build`
