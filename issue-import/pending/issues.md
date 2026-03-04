## Issue: Keycloak local setup (docker-compose) + realm import
Labels: chore, infra, auth, user, new

## Description
Add Keycloak to local development so backend + UI can authenticate against a real IdP. Include a committed realm import to keep setup reproducible.

## Acceptance Criteria
- [ ] `docker-compose.yml` includes a Keycloak service
- [ ] Realm export file is committed (e.g. `infra/keycloak/realm-export.json`)
- [ ] Realm contains:
  - realm `task-manager`
  - client `task-manager-api`
  - roles `USER`, `ADMIN`
  - at least 2 test users (e.g. `user1`, `user2`) for local dev
- [ ] README documents how to:
  - start Keycloak
  - obtain a token (curl example)
  - login using the UI (once added)

---

## Issue: Backend as OAuth2 Resource Server (Keycloak JWT) + /api/v1/me
Labels: enhancement, backend, auth, api, user, tests, new

## Description
Replace any basic auth/JWT homemade logic with Keycloak-based JWT validation. Provide a simple identity endpoint to verify authentication and role mapping.

## Acceptance Criteria
- [ ] Backend validates Bearer JWT issued by Keycloak (issuer URI configurable)
- [ ] Roles are mapped to Spring authorities (`ROLE_USER`, `ROLE_ADMIN`)
- [ ] Endpoint exists: `GET /api/v1/me` (protected)
- [ ] `/api/v1/me` returns:
  - user identifier (email/username from token)
  - roles
- [ ] Unauthorized requests return `401`
- [ ] Integration tests (Testcontainers + RestAssured):
  - start Postgres + Keycloak containers
  - import realm
  - obtain token from Keycloak
  - `/api/v1/me` with token -> 200
  - `/api/v1/me` without token -> 401

---

## Issue: API versioning baseline (/api/v1) + Swagger bearer auth
Labels: chore, backend, api, tests, new

## Description
Standardize all endpoints under `/api/v1/...` and ensure Swagger UI is compatible with Keycloak JWT authentication.

## Acceptance Criteria
- [ ] All REST controllers use `/api/v1` prefix
- [ ] Swagger/OpenAPI configured with Bearer JWT security scheme
- [ ] Swagger UI loads and shows versioned endpoints only
- [ ] Requests to non-versioned routes (e.g. `/api/projects`) return `404`
- [ ] Integration test verifies:
  - `/api/v1/me` works with token
  - `/api/me` returns 404

---

## Issue: Projects flow (CRUD + ownership) /api/v1
Labels: enhancement, backend, api, database, project, tests, new

## Description
Implement project CRUD for authenticated users, enforcing strict ownership (a user can access only their own projects).

## Acceptance Criteria
- [ ] Flyway migration creates `projects` table with FK to `users` and index on `owner_id`
- [ ] Endpoints (protected):
  - POST `/api/v1/projects`
  - GET `/api/v1/projects`
  - GET `/api/v1/projects/{id}`
  - PATCH `/api/v1/projects/{id}`
  - DELETE `/api/v1/projects/{id}`
- [ ] Validation:
  - name is required, length constraints enforced
- [ ] Ownership:
  - User A can access own projects
  - User B accessing User A project returns `404`
- [ ] Integration tests (Testcontainers + RestAssured):
  - userA create/list/get/update/delete -> expected codes
  - userB get/update/delete userA project -> 404

---

## Issue: Tasks flow (CRUD + filters + paging + optimistic locking) /api/v1
Labels: enhancement, backend, api, database, task, tests, new

## Description
Implement tasks within projects with filtering/pagination and optimistic locking to prevent lost updates.

## Acceptance Criteria
- [ ] Flyway migration creates `tasks` table with indexes:
  - project_id, due_at, status, (project_id,status)
  - includes `version` column for optimistic locking
- [ ] Endpoints (protected):
  - POST `/api/v1/projects/{projectId}/tasks`
  - GET `/api/v1/projects/{projectId}/tasks?status=&dueFrom=&dueTo=&q=&page=&size=`
  - PATCH `/api/v1/tasks/{taskId}` (requires `version`)
  - POST `/api/v1/tasks/{taskId}/complete`
  - DELETE `/api/v1/tasks/{taskId}`
- [ ] Behavior:
  - complete sets status DONE and completedAt
  - update with stale version returns `409 Conflict`
  - non-owner access returns `404`
- [ ] Integration tests (Testcontainers + RestAssured):
  - create task, list with filters/paging
  - update with correct version -> 200
  - update with stale version -> 409
  - userB cannot access userA task -> 404

---

## Issue: Reminders pipeline (enqueue due-soon + async send + idempotency)
Labels: enhancement, backend, database, reminder, tests, new

## Description
Add reminders as a reliable pipeline: scheduler enqueues reminders for due-soon tasks and an async worker sends them. Must be idempotent.

## Acceptance Criteria
- [ ] Flyway migration creates `task_reminders` table with unique constraint `(task_id, scheduled_at)`
- [ ] Enqueue logic:
  - finds tasks due within configured window
  - creates PENDING reminders
  - safe to run repeatedly (no duplicates)
- [ ] Async sender:
  - transitions PENDING -> SENT on success
  - increments attempts and stores error on failure
- [ ] Endpoint (protected) for visibility:
  - GET `/api/v1/reminders?status=PENDING|SENT|FAILED`
- [ ] Integration tests (Testcontainers + RestAssured):
  - create due-soon task
  - trigger enqueue deterministically in tests (test profile helper)
  - verify exactly 1 reminder created
  - await until SENT
  - run enqueue again -> still 1 reminder (idempotency)

---

## Issue: Export tasks to CSV via async job (status + download)
Labels: enhancement, backend, api, database, export, reporting, tests, new

## Description
Implement export as an async job so user can request export, poll status, and download the resulting CSV.

## Acceptance Criteria
- [ ] Flyway migration creates `export_jobs` table with status/type fields and indexes
- [ ] Endpoints (protected):
  - POST `/api/v1/exports` (projectId, type=CSV)
  - GET `/api/v1/exports/{jobId}` (status)
  - GET `/api/v1/exports/{jobId}/download` (CSV when DONE)
- [ ] Behavior:
  - POST returns `202` with jobId quickly
  - async worker transitions PENDING -> RUNNING -> DONE/FAILED
  - download before DONE returns `409`
  - non-owner access returns `404`
- [ ] Integration tests (Testcontainers + RestAssured):
  - create tasks
  - request export -> jobId
  - await status DONE
  - download CSV -> contains expected headers and task titles

---

## Issue: CI/CD pipeline runs unit + integration tests (Testcontainers + RestAssured)
Labels: chore, ci, tests, new

## Description
Add GitHub Actions workflow to build and run unit tests + integration tests on each push and pull request.

## Acceptance Criteria
- [ ] Maven configured so ITs run via Failsafe (`*IT` naming)
- [ ] GitHub Actions workflow runs `mvn verify`
- [ ] Integration tests run in CI (Docker available)
- [ ] Maven dependencies cached
- [ ] CI fails if any unit/IT fails

---

## Issue: UI bootstrap (React JS) + Keycloak login/logout + /api/v1/me
Labels: enhancement, ui, auth, user, new

## Description
Create a minimal React (JavaScript) UI, integrate Keycloak, and verify backend communication.

## Acceptance Criteria
- [ ] React app created (Vite + React + JS)
- [ ] Login and logout work via Keycloak
- [ ] After login, UI calls `GET /api/v1/me` and displays username + roles
- [ ] API base URL configurable via env (dev-friendly)

---

## Issue: UI Projects page (list + create + delete)
Labels: enhancement, ui, project, new

## Description
Implement a Projects page that supports basic project management.

## Acceptance Criteria
- [ ] List projects from GET `/api/v1/projects`
- [ ] Create project via POST `/api/v1/projects`
- [ ] Delete project via DELETE `/api/v1/projects/{id}`
- [ ] Shows validation and auth errors in UI

---

## Issue: UI Tasks page (list + filters + create + complete + edit with 409 handling)
Labels: enhancement, ui, task, new

## Description
Implement Tasks UI including filtering/pagination and conflict handling for optimistic locking.

## Acceptance Criteria
- [ ] List tasks with filters/paging
- [ ] Create task
- [ ] Complete task
- [ ] Edit task using PATCH with `version`
- [ ] If API returns 409, UI shows conflict message and reloads latest task

---

## Issue: UI Exports page (create export + poll status + download CSV)
Labels: enhancement, ui, export, reporting, new

## Description
Implement export flow in UI: request export job, poll status, and download CSV.

## Acceptance Criteria
- [ ] Create export via POST `/api/v1/exports`
- [ ] Poll GET `/api/v1/exports/{jobId}` until DONE/FAILED
- [ ] Enable download only when DONE
- [ ] Show failure message if FAILED