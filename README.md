# task-manager-pro

Spring Boot 3 (Java 17) skeleton for a production-ready Task Manager project.

## Run (requires PostgreSQL)
1) Start PostgreSQL (later you'll add docker-compose; for now you can run locally)
2) Configure env vars (optional)
- DB_URL (default: jdbc:postgresql://localhost:5432/task_manager)
- DB_USER (default: task_manager)
- DB_PASSWORD (default: task_manager)

3) Run:
```bash
mvn spring-boot:run
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
- [ ] Profiles: dev / test / prod + externalized config

### Testing & Quality
- [ ] Unit tests for services (JUnit + Mockito)
- [ ] Integration tests (Spring Boot Test + Testcontainers)
- [ ] API contract checks for key endpoints

### Delivery
- [ ] Docker Compose for local run (app + PostgreSQL)
- [ ] GitHub Actions CI: build + test on every push
