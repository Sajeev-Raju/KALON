# Docker setup for KALON

## Quick start

From the project root:

```bash
docker compose up --build
```

- **Frontend (store):** http://localhost:5173  
- **Admin dashboard:** http://localhost:5174  
- **Backend API:** http://localhost:8082 (port 8082 to avoid conflict with local 8080)
- **Database:** Not exposed on host (only backend can reach it). To use port 5432, set `ports: ["5432:5432"]` for `db` in `docker-compose.yml` and ensure nothing else uses 5432.  

The frontends are built with `VITE_API_URL=http://localhost:8082/api`, so the browser talks to the backend on port 8082.

## Services

| Service          | Image / Build     | Port  | Description                    |
|------------------|-------------------|------|--------------------------------|
| `db`             | postgres:16-alpine| 5432 | PostgreSQL (kalon_db)          |
| `backend`        | backend/Dockerfile | 8082 | Spring Boot API                |
| `frontend`        | frontend/Dockerfile| 5173 | Customer-facing React app       |
| `admin-frontend`  | admin-frontend/Dockerfile | 5174 | Admin React app        |

## Environment

- **Database:** User `kalon`, password `kalon`, database `kalon_db`. Override with `POSTGRES_*` for `db` and `SPRING_DATASOURCE_*` for `backend` if needed.
- **Backend:** Optional env vars (see `backend/src/main/resources/application.properties`): `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `CORS_ALLOWED_ORIGINS`, Razorpay keys, SMTP settings, etc. Set them in `docker-compose.yml` under `backend.environment` or via an env file.
- **Frontends:** Build arg `VITE_API_URL` (default `http://localhost:8082/api` in this compose). For a different API host (e.g. production), rebuild with `--build-arg VITE_API_URL=https://api.example.com/api`.

## Volumes

- `postgres_data`: PostgreSQL data (persists across restarts).
- `backend_uploads`: Backend upload directory (persists).

## Fresh database / backend won't start

The backend expects an existing schema (Flyway migrations V2+ assume base tables exist; there is no V1). Options:

1. **Use an existing DB:** Point `SPRING_DATASOURCE_URL` to a database that already has the KALON schema.
2. **First-time schema:** Run the backend once with env `SPRING_JPA_HIBERNATE_DDL_AUTO=update` and `SPRING_FLYWAY_ENABLED=false` in `docker-compose.yml` so Hibernate creates the schema, then switch back to `validate` and Flyway enabled for later runs.

If the backend exits with a `UserRepository.countActiveUsers()` / `User.Role.USER` validation error, that is an application query bug and needs a code fix in the repository.

## Build only

```bash
docker compose build
```

## Run in background

```bash
docker compose up -d --build
```

## Stop and remove volumes

```bash
docker compose down -v
```
