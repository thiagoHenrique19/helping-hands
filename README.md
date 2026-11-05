# Helping Hands

REST API connecting NGOs, volunteers and donors. NGOs publish fundraising campaigns and volunteer opportunities, donors contribute (optionally anonymously), volunteers apply, and everyone gets transparent impact reports.

**Stack:** Java 21, Spring Boot 3.3, Spring Data JPA/Hibernate, PostgreSQL 16, Flyway, Spring Security (stateless JWT), springdoc-openapi, Testcontainers, JUnit 5, Mockito, AssertJ, Maven, Docker, GitHub Actions.

## Features
- **Organizations**: unique e-mail, phone, website, verification flag, update, paginated search by name.
- **Campaigns**: goal, optional deadline (`endsAt`), category, update (only while `ACTIVE` and the goal must stay above the raised amount), cancel, paginated search with filters (organization, status, category, title) and sorting. A scheduled job marks campaigns past their deadline as `EXPIRED`; donations to expired campaigns are rejected.
- **Donations**: payment method (`PIX`, `CREDIT_CARD`, `BOLETO`), minimum amount (1.00), optional message, anonymous donors (masked in public listings), receipt code `DON-########` from a PostgreSQL sequence, public paginated feed and top donors (native SQL).
- **Volunteering**: opportunities (slots, remote flag, period, `OPEN/FULL/CLOSED/CANCELLED`) and applications (`PENDING/APPROVED/REJECTED/WITHDRAWN`). No duplicate application per e-mail, approving fills slots and automatically marks the opportunity `FULL`, withdrawing an approved application frees the slot, closed/cancelled opportunities reject applications.
- **Domain events** (`CampaignCompleted`, `VolunteerApplicationApproved`, `OpportunityFilled`) published through a core port and handled by logging listeners.
- **Impact analytics** (native PostgreSQL: CTEs, `FILTER`, `generate_series` + `date_trunc`, `rank() OVER`): organization dashboard, donations per month, top campaigns, campaign progress ranking, payment method breakdown and a platform-wide summary.
- **Security**: stateless JWT (HMAC) with BCrypt passwords, roles `ADMIN`, `ORGANIZATION`, `VOLUNTEER` and ownership checks.
- **API quality**: OpenAPI + Swagger UI, RFC 7807 `application/problem+json` errors, `X-Request-Id` correlation id in responses, logs and error bodies, Actuator health/info.

## Architecture (multi-module Maven, ports & adapters)
```
helping-hands/
├── core/            domain entities, business rules, pagination model, domain events,
│                    ports (in = use cases + DTOs, out = repositories/gateways). No Spring.
├── application/     use case implementations (services) and mappers. Depends only on core.
└── infrastructure/  Spring Boot app: REST controllers, security, JPA/JDBC adapters,
                     Flyway migrations, scheduler, event listeners, configuration.
```

```mermaid
flowchart LR
    Client([HTTP client]) --> Filter[RequestIdFilter + Spring Security]
    Filter --> Controllers[REST controllers]
    Controllers -->|use case interfaces| Services[Application services]
    Services --> Domain[(Domain model<br/>core)]
    Services -->|out ports| Adapters[Persistence adapters<br/>JPA + JDBC native SQL]
    Services -->|DomainEventPublisher| Events[Spring events -> listeners]
    Adapters --> PG[(PostgreSQL)]
    Scheduler[@Scheduled job] -->|ExpireCampaignsUseCase| Services
```
Dependencies point inward: `infrastructure -> application -> core`; the compiler enforces it because each layer is a Maven module. Controllers only know use case interfaces; pagination (`PageQuery`/`PageResult`) is framework independent in `core` and translated to Spring Data in the adapters.

## Run
Requires Java 21 and Docker. Always use the Maven wrapper (`./mvnw`).

```bash
# Whole stack (PostgreSQL + app, with healthchecks) on http://localhost:8080
APP_ADMIN_EMAIL=admin@local.dev APP_ADMIN_PASSWORD=change-me-now docker compose up -d --build

# Or: only the database, app from source
docker compose up -d postgres
./mvnw spring-boot:run -pl infrastructure

# Tests (integration tests start PostgreSQL with Testcontainers)
./mvnw verify
```
Swagger UI: <http://localhost:8080/swagger-ui.html> · OpenAPI JSON: `/v3/api-docs` · Health: `/actuator/health`.

### Configuration (environment variables)
| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | PostgreSQL connection | local `helpinghands` database |
| `JWT_SECRET` | HMAC secret (min. 32 bytes). **Required** in `hmg` and `prod` profiles | dev-only value in the default profile |
| `APP_ADMIN_EMAIL`, `APP_ADMIN_PASSWORD` | Creates the first `ADMIN` on startup, only when no users exist | disabled |
| `SPRING_PROFILES_ACTIVE` | `hmg` (homologation) or `prod` | default (local) |
| `DB_POOL_SIZE` | Hikari pool size (`prod`) | 10 |

Optional: `helpinghands.campaigns.expiration-cron` changes the schedule of the campaign expiration job (default every 5 minutes).

## Authentication
1. `POST /api/auth/register` with `{"email","password","role"}` where role is `VOLUNTEER` or `ORGANIZATION` (admins only come from the bootstrap variables).
2. `POST /api/auth/login` returns `{"accessToken","tokenType":"Bearer","expiresIn"}`.
3. Send `Authorization: Bearer <token>`.

An `ORGANIZATION` user becomes the owner of the organization it creates with `POST /api/organizations` (one per user). Donating and applying to opportunities never requires an account.

## Endpoints
Access legend: **public** = no token, **owner** = `ADMIN` or the `ORGANIZATION` user that owns the organization, **admin** = `ADMIN`.

| Method & path | Description | Access |
|---|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Account registration / login | public |
| `POST /api/organizations` | Register an organization | `ADMIN`, `ORGANIZATION` |
| `GET /api/organizations?name=&page=&size=&sortBy=&direction=` | Search organizations | public |
| `GET /api/organizations/{id}` | Get an organization | public |
| `PUT /api/organizations/{id}` | Update | owner |
| `POST /api/organizations/{id}/verify` | Mark as verified | owner |
| `POST /api/organizations/{id}/campaigns` | Create a campaign | owner |
| `GET /api/campaigns?organizationId=&status=&category=&title=&page=&size=&sortBy=&direction=` | Search campaigns | public |
| `GET /api/campaigns/{id}` | Get a campaign | public |
| `PUT /api/campaigns/{id}` | Update an active campaign | owner |
| `POST /api/campaigns/{id}/cancel` | Cancel a campaign | owner |
| `POST /api/campaigns/{id}/donations` | Donate | public |
| `GET /api/campaigns/{id}/donations` | Public donation feed (anonymous donors masked) | public |
| `GET /api/campaigns/{id}/top-donors?limit=` | Top identified donors | public |
| `POST /api/organizations/{id}/opportunities` | Publish a volunteer opportunity | owner |
| `GET /api/opportunities?organizationId=&status=&remote=&upcoming=` | Search opportunities | public |
| `GET /api/opportunities/{id}` | Get an opportunity | public |
| `POST /api/opportunities/{id}/close`, `/cancel` | Close / cancel | owner |
| `POST /api/opportunities/{id}/applications` | Apply | public |
| `GET /api/opportunities/{id}/applications?status=` | List applications (contains volunteer e-mails) | owner |
| `POST /api/applications/{id}/approve`, `/reject` | Decide an application | owner |
| `POST /api/applications/{id}/withdraw` | Withdraw an application | public |
| `GET /api/organizations/{id}/analytics/dashboard` | Organization dashboard | owner |
| `GET /api/organizations/{id}/analytics/donations-by-month?from=&to=` | Donations per month (ISO dates, max 60 months) | owner |
| `GET /api/organizations/{id}/analytics/top-campaigns?limit=` | Top campaigns by raised amount | owner |
| `GET /api/organizations/{id}/analytics/campaign-progress` | Campaign progress ranking (`rank() OVER`) | owner |
| `GET /api/organizations/{id}/analytics/payment-methods` | Donations by payment method | owner |
| `GET /api/analytics/summary`, `/top-campaigns`, `/payment-methods` | Platform-wide reports | admin |

Paginated endpoints return `{"content":[...],"page","size","totalElements","totalPages"}` (`size` max 100).

### Errors
All errors are RFC 7807 problem details (`application/problem+json`):
```json
{
  "type": "urn:helping-hands:problem:validation-failed",
  "title": "Validation failed",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/auth/register",
  "errors": [{"field": "email", "message": "must be a well-formed email address"}],
  "timestamp": "2026-11-05T18:00:00Z",
  "requestId": "0b6f4f0e-..."
}
```
Status codes: `400` invalid input, `401` missing/invalid token, `403` not allowed, `404` not found, `409` conflict (duplicates, concurrent modification), `422` business rule violated.

## Database
Schema is versioned with Flyway (`infrastructure/src/main/resources/db/migration`) and validated by Hibernate (`ddl-auto=validate`). Never edit an applied migration; add a new `V<n>__description.sql`.

## Testing strategy
- Domain and service unit tests (JUnit 5, Mockito, AssertJ).
- `@WebMvcTest` slices including the real security rules.
- Testcontainers integration tests against PostgreSQL 16 for persistence, native SQL analytics and end-to-end flows.

## Branching model and workflow
| Branch | Purpose |
|---|---|
| `main` | Production. Receives only release PRs from `hmg`; tagged `vX.Y.Z`. |
| `hmg` | Homologation (integration branch), always green. |
| `feat/*` | One branch per feature, created from `hmg`, merged through a pull request. |

Commits use English imperative messages with the `feat:` or `fix:` prefix. CI (`.github/workflows/ci.yml`) runs `./mvnw -B verify` on pushes and pull requests to `main` and `hmg`. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full flow.

## Roadmap
Payment gateway integration, e-mail notifications driven by the domain events, refresh tokens.
