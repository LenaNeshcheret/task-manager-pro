# task-manager-pro

Spring Boot 3 (Java 21) skeleton for a production-ready Task Manager project.

## Run with Docker (app + PostgreSQL)
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
```

4) Stop all containers:
```bash
docker compose down
```

## Run app locally (optional)
1) Start only PostgreSQL:
```bash
docker compose up -d postgres
```

2) Run app with Maven:
```bash
mvn spring-boot:run
```

## Profiles
- `dev` (default): uses shared PostgreSQL settings from `application.yml` and `.env`
- `prod`: same shared PostgreSQL settings, with SQL formatting disabled
- `test`: same shared PostgreSQL settings, overrides only credentials to `test` / `test` (auto-used for `mvn test`)

Run with `prod` profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Endpoints
- GET `/api/hello` (no auth)
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
- [ ] Docker Compose for local run (app + PostgreSQL)
- [ ] GitHub Actions CI: build + test on every push
