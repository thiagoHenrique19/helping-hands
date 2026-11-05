# Helping Hands

REST API connecting NGOs, volunteers and donors. NGOs create fundraising campaigns, donors contribute, and campaigns complete automatically when the goal is reached.

**Stack:** Java 21, Spring Boot 3, Spring Data JPA/Hibernate, PostgreSQL, Flyway, Testcontainers, JUnit 5, Mockito, Maven.

## Architecture (multi-module Maven, ports & adapters)
```
helping-hands/
├── core/            domain, business rules, ports (in = use cases + DTOs, out = repositories)
├── application/     use case implementations (services), mappers
└── infrastructure/  Spring Boot app: REST controllers, error handling, JPA adapters
```
Dependencies point inward: `infrastructure -> application -> core`. The compiler enforces it, since each layer is its own Maven module.

## Run
Requires Java 21 and Docker. Schema is versioned with Flyway (`infrastructure/src/main/resources/db/migration`).
```
docker compose up -d                      # PostgreSQL on localhost:5432
./mvnw test                               # integration tests use Testcontainers (real PostgreSQL)
./mvnw spring-boot:run -pl infrastructure
```
Connection can be overridden with `DB_URL`, `DB_USER` and `DB_PASSWORD`.

## Endpoints
- `POST /api/organizations`, `GET /api/organizations/{id}`
- `POST /api/organizations/{id}/campaigns`, `GET /api/campaigns`, `GET /api/campaigns/{id}`
- `POST /api/campaigns/{id}/donations`

## Roadmap
Volunteer opportunities and applications, authentication (JWT), payment gateway.
