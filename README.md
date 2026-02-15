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
