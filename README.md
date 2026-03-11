# task-manager-pro

Spring Boot 3 (Java 21) skeleton for a production-ready Task Manager project.

## Run with Docker (app + PostgreSQL + Keycloak)
1) Copy env template:
```bash
cp .env.example .env
```

2) Build and start all services:
```bash
docker compose up -d --build
```

3) Open:
```text
http://localhost:8080/swagger-ui.html
http://localhost:8081 (Keycloak)
```

4) Stop all containers:
```bash
docker compose down
```

## Run app locally (optional)
1) Start PostgreSQL + Keycloak:
```bash
docker compose up -d postgres keycloak
```

2) Run app with Maven:
```bash
mvn spring-boot:run
```

## Keycloak local setup
- Realm import file: `infra/keycloak/realm-export.json`
- Imported realm: `task-manager`
- Imported client: `task-manager-api`
- Imported realm roles: `USER`, `ADMIN`
- Test users:
  - `user1` / `user1pass` (`USER`)
  - `user2` / `user2pass` (`USER`, `ADMIN`)
- Admin console:
  - URL: `http://localhost:8081`
  - Username: `admin` (or `KEYCLOAK_ADMIN` from `.env`)
  - Password: `admin` (or `KEYCLOAK_ADMIN_PASSWORD` from `.env`)
- Backend JWT issuer env: `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI`
  - Default value in `.env.example`: `http://host.docker.internal:8081/realms/task-manager`

### Obtain an access token (curl)
```bash
curl --request POST 'http://localhost:8081/realms/task-manager/protocol/openid-connect/token' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=task-manager-api' \
  --data-urlencode 'username=user1' \
  --data-urlencode 'password=user1pass'
```

### Call protected endpoint `/api/v1/me`
```bash
TOKEN=$(curl --silent --request POST 'http://localhost:8081/realms/task-manager/protocol/openid-connect/token' \
  --header 'Content-Type: application/x-www-form-urlencoded' \
  --data-urlencode 'grant_type=password' \
  --data-urlencode 'client_id=task-manager-api' \
  --data-urlencode 'username=user2' \
  --data-urlencode 'password=user2pass' | jq -r '.access_token')

curl --request GET 'http://localhost:8080/api/v1/me' \
  --header "Authorization: Bearer ${TOKEN}"
```
Expected response includes token identity + roles, for example:
```json
{
  "email": "user2@example.com",
  "roles": ["ROLE_ADMIN", "ROLE_USER"]
}
```

Without a bearer token, `/api/v1/me` returns `401 Unauthorized`.

### Login from UI (once UI is added)
1) Configure your frontend OIDC settings:
   - Issuer: `http://host.docker.internal:8081/realms/task-manager`
   - Client ID: `task-manager-api`
   - Flow: Authorization Code with PKCE (`S256`)
2) Run the UI on `http://localhost:3000` or `http://localhost:5173` (both are preconfigured redirect URIs).
3) Start login from UI and sign in with one of the test users above.

## Profiles
- `dev` (default): uses shared PostgreSQL settings from `application.yml` and `.env`
- `prod`: same shared PostgreSQL settings, with SQL formatting disabled
- `test`: same shared PostgreSQL settings, overrides only credentials to `test` / `test` (auto-used for `mvn test`)

Run with `prod` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Endpoints
- GET `/api/v1/health` (no auth)
- GET `/api/health` (no auth, legacy alias)
- GET `/api/v1/me` (requires Bearer token)
- Swagger UI: `/swagger-ui.html`
- Actuator: `/actuator/health`

## Build jar
```bash
mvn clean package
```
Jar will be in `target/`.


## 🚧 Roadmap (Features coming soon)

Planned improvements and production-ready features that will be implemented next:

### Core Product
- [ ] User registration & login (JWT)
- [ ] Role-based access control (USER / ADMIN)
- [ ] CRUD: Projects & Tasks
- [ ] Task fields: due date, priority, status, tags
- [ ] Filtering, sorting & pagination for task lists
- [ ] Validation + consistent API error responses

### Scheduling & Async
- [ ] Reminder scheduler: find tasks due soon and enqueue notifications
- [ ] Daily cleanup scheduler: archive completed tasks older than X days
- [ ] Async notification delivery (non-blocking)
- [ ] Async export job (CSV, later PDF)

### Persistence & Data Quality
- [ ] PostgreSQL + Spring Data JPA / Hibernate
- [ ] Flyway DB migrations
- [ ] Optimistic locking for concurrent updates
- [ ] Proper indexing for common queries

### API & Documentation
- [ ] OpenAPI / Swagger UI
- [ ] Versioned REST API (`/api/v1`)
- [ ] Health endpoints & system info via Spring Boot Actuator

### Observability & Ops
- [ ] Structured logging + request correlation IDs
- [ ] Metrics (Actuator) and readiness/liveness checks
- [x] Profiles: dev / test / prod + externalized config

### Testing & Quality
- [ ] Unit tests for services (JUnit + Mockito)
- [ ] Integration tests (Spring Boot Test + Testcontainers)
- [ ] API contract checks for key endpoints

### Delivery
- [x] Docker Compose for local run (app + PostgreSQL + Keycloak)
- [ ] GitHub Actions CI: build + test on every push
