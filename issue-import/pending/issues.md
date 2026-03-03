## Issue: Keycloak local setup (docker-compose) + realm import
Labels: chore, infra, auth, user

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