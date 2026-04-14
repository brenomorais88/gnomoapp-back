# Engineering Rules and Conventions

## Scope

These rules are mandatory for all backend code in this repository.

## Language and Communication

- Code, comments, commits, docs, API fields, and migration descriptions must be in English only.

## Modular Monolith Rules

- Keep one deployable backend service.
- Organize by feature first, then by layer.
- Do not bypass module boundaries through direct infrastructure access from routes.
- Shared code must stay generic and not contain feature-specific rules.

## Layer Contracts

- `api` depends on `application` and `shared`.
- `application` depends on `domain` and repository interfaces.
- `domain` depends only on Kotlin stdlib and shared value abstractions.
- `infrastructure` implements ports/interfaces declared by upper layers.

Forbidden:
- business logic in routes
- HTTP concerns in use cases/services
- persistence behavior outside repositories

## Naming Conventions

- Classes: `PascalCase`
- Functions/variables/properties: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: lowercase, no underscores
- SQL tables/columns/indexes: `snake_case`

## DTO Conventions

- Request DTO names: `<Action><Feature>Request`
- Response DTO names: `<Feature><Context>Response`
- API DTOs must remain in `api` layer only.
- Domain entities/value objects must not be exposed directly in route responses.

## Repository Conventions

- Domain-facing interfaces in `application` or `domain` packages.
- Exposed-based implementations in `infrastructure`.
- Repositories only handle persistence operations and mapping.
- Transactions are coordinated by application use cases, not controllers.

## Use Case / Service Conventions

- One use case class per business action (`CreateAccountUseCase`, etc).
- Public entrypoint should be `execute(...)`.
- Use cases return typed result objects or throw mapped domain exceptions.
- Use cases must be deterministic and testable with mocked ports.

## Error Handling Conventions

- Centralized exception-to-HTTP mapping in shared API error handler.
- Standard response model:
  - `timestamp: Instant`
  - `path: String`
  - `errorCode: String`
  - `message: String`
  - `details: Map<String, String>?`
  - `traceId: String?`
- Never leak internal stack traces in API responses.

## Validation Conventions

- Input shape and required-field validation in `api`.
- Cross-field and business-rule validation in `application/domain`.
- Validation messages must be explicit and user-safe.

## Date/Time Conventions

- `LocalDate` for due/start/end dates.
- `Instant` for audit timestamps (`createdAt`, `updatedAt`).
- UTC is mandatory at system boundaries and scheduling.

## Money Conventions

- Always use `BigDecimal` for money.
- Zero is valid and must be supported.
- Never use `Double`/`Float` for monetary values.
- Standard currency and scale policy should be centralized in shared money utilities.

## Recurrence and Occurrence Conventions

- Supported recurrence: `UNIQUE`, `DAILY`, `WEEKLY`, `MONTHLY`.
- Keep occurrence horizon generated to 24 months ahead.
- Trigger immediate regeneration on account create/edit/activate.
- Daily scheduler maintains horizon.
- Automatic jobs must not mutate paid or past occurrences.
- Occurrence-level amount override must be supported.

## Migration Conventions

- Use Flyway for all schema changes.
- Files: `V<version>__<description>.sql`.
- No modification of already-applied migration files.
- Keep migrations backward-compatible when possible.

## Seed Conventions

- Seeds live in dedicated migration or explicit seed runner.
- Seed data must be deterministic and idempotent.
- No production-only secrets or personal data in seeds.

## Scheduler Conventions

- Scheduler runs once daily in UTC.
- Jobs must be idempotent and re-entrant.
- Job execution must produce structured logs and error alerts.
- Long jobs must support safe retries.

## Testing Conventions

- Unit tests for use cases, domain services, and mappers.
- Integration tests for routes + persistence flows where needed.
- Given/When/Then naming style encouraged.
- Coverage threshold: `>= 70%` minimum.

## Quality Gates

Every change must satisfy:
- formatting/lint (if configured)
- all tests passing
- coverage `>= 70%`
- `./gradlew build` passing
