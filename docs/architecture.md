# Backend Architecture

## Objective

This document defines the architecture direction for `daily-back`, a Kotlin backend for personal/family bill management designed to evolve into a SaaS product.

The architecture favors:
- high readability
- low accidental complexity
- clear module boundaries
- safe evolution over time

## Architectural Style

The backend follows a **modular monolith** with:
- one deployable runtime
- feature-first code organization
- layered architecture inside each feature

Each feature is internally isolated by package boundaries and explicit interfaces, so extraction to microservices remains possible in the future without rewriting core business rules.

## Feature-First + Layered Structure

Top-level package:
- `com.dailyback`

Proposed package layout:

```text
com.dailyback
  ├─ app
  │   ├─ config
  │   ├─ di
  │   └─ bootstrap
  ├─ features
  │   ├─ categories
  │   │   ├─ api
  │   │   ├─ application
  │   │   ├─ domain
  │   │   └─ infrastructure
  │   ├─ accounts
  │   │   ├─ api
  │   │   ├─ application
  │   │   ├─ domain
  │   │   └─ infrastructure
  │   ├─ accountoccurrences
  │   │   ├─ api
  │   │   ├─ application
  │   │   ├─ domain
  │   │   └─ infrastructure
  │   └─ home
  │       ├─ api
  │       ├─ application
  │       ├─ domain
  │       └─ infrastructure
  └─ shared
      ├─ api
      ├─ domain
      ├─ infrastructure
      ├─ validation
      ├─ errors
      ├─ time
      └─ money
```

## Layer Responsibilities

### `api`
- Ktor routes and request/response DTOs
- request parsing, validation trigger, and output mapping
- must not contain business rules

### `application`
- use cases and orchestration
- transaction boundaries
- coordinates domain objects and repositories
- receives/returns application models (not Exposed entities)

### `domain`
- entities, value objects, domain services, domain rules
- pure business behavior with no HTTP or persistence dependencies

### `infrastructure`
- Exposed table definitions and repository implementations
- Flyway integration, scheduler adapters, external integrations
- mapping between persistence and domain models

### `shared`
- cross-cutting utilities used by multiple features
- standardized error model and exception hierarchy
- time and money helper policies

## Business-Driven Module Boundaries

Primary feature modules:
- `categories`
- `accounts`
- `accountoccurrences`
- `home` (dashboard query module)

Cross-feature rules:
- `Categories` are mandatory for accounts.
- Recurrence types: `UNIQUE`, `DAILY`, `WEEKLY`, `MONTHLY`.
- Occurrence generation window: rolling 24 months.
- Generation must run immediately on account create/edit/activate.
- Daily scheduled reconciliation must ensure next 24 months are generated.
- Paid occurrences are immutable by automatic recalculation jobs.
- Past occurrences are immutable by automatic recalculation jobs.
- Specific occurrence amount overrides must be supported without changing base account amount.
- Account delete is allowed only when there is no relevant history; otherwise account must be deactivated.

## Data and Type Standards

Mandatory standards:
- identifiers: `UUID`
- money values: `BigDecimal` (scale policy defined in engineering rules)
- due/start/end dates: `LocalDate`
- timestamps (`createdAt`, `updatedAt`, audit): `Instant`
- database naming: `snake_case` for tables and columns

## API Error Standard

The system uses a unified error payload:

```json
{
  "timestamp": "2026-04-14T10:00:00Z",
  "path": "/v1/accounts",
  "errorCode": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": {
    "categoryId": "must not be null"
  },
  "traceId": "e4f9a8c88fbc4e74"
}
```

Field meaning:
- `timestamp`: UTC instant
- `path`: request path
- `errorCode`: stable machine-friendly code
- `message`: user-safe summary
- `details`: optional field-level or business-rule details
- `traceId`: optional correlation id for observability

## Persistence and Migration Direction

- Flyway is mandatory for schema changes.
- Every migration is immutable after merged.
- Naming format: `V<version>__<description>.sql`
- Seed scripts are deterministic and idempotent.

## Scheduler Direction

Occurrence-generation scheduler design:
- runs daily in UTC
- checks all active recurring accounts
- guarantees generation horizon up to `now + 24 months`
- never mutates paid or past occurrences automatically
- must be safe for retries and idempotent

## Quality Gates

The repository must satisfy all of the following before merge:
- tests pass
- unit test coverage is `>= 70%`
- full build passes

These gates are enforced in CI and must also be verifiable locally.
