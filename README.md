# OfficeArcade

OfficeArcade is a workplace-friendly desktop gaming platform for short break-time multiplayer sessions inside a company environment. Employees use company-managed accounts to join or host game rooms, compete in quick games, earn progression, and interact through reputation systems like Respect and Karma, while admins control access, departments, play rules, and cooldown policies.

## Stack Overview

- Desktop shell: Tauri
- Frontend: React + TypeScript + Vite + Tailwind CSS
- Visual/game layer placeholder: PixiJS dependency included (no gameplay implementation yet)
- Backend: Spring Boot (Java 21)
- Persistence (later): PostgreSQL
- Realtime (later): WebSocket

## Monorepo Structure

- `docs/` - concept, architecture, roadmap, and API placeholders
- `officearcade-client/` - Tauri + React + TypeScript desktop client app
- `officearcade-server/` - Spring Boot backend with auth/security and admin-user management APIs
- `officearcade-shared/` - shared contracts/types/docs placeholders for future PTs
- `assets/` - branding/cosmetics/avatars/mockups placeholders

## OA-PT03 Implemented Scope

- Spring Security + JWT auth (`ADMIN`, `EMPLOYEE`) with protected route guards
- Shared in-memory user account service used by:
  - authentication
  - admin user management
- Public/system endpoints:
  - `GET /api/health`
- Auth endpoints:
  - `POST /api/auth/login`
  - `GET /api/auth/me`
- Admin-only user management endpoints:
  - `GET /api/admin/users` (supports `search`, `role`, `active`)
  - `GET /api/admin/users/{id}`
  - `POST /api/admin/users`
  - `PUT /api/admin/users/{id}`
  - `POST /api/admin/users/{id}/activate`
  - `POST /api/admin/users/{id}/deactivate`
  - `POST /api/admin/users/{id}/reset-password`
- Client auth and shell:
  - login page
  - token persistence across reloads
  - startup session restore with `/api/auth/me`
  - logout flow
  - protected routing + role guards
- Admin user module UI:
  - admin-only navigation item
  - user list with search/filter
  - create user flow
  - user detail/edit panel
  - activate/deactivate actions
  - reset password action

## Admin User Safety Rules

- Employee users do not see admin user navigation and cannot access admin user routes/APIs.
- Backend blocks unsafe operations that would remove the last active `ADMIN` account.

## Auth Flow Summary

1. User submits credentials to `POST /api/auth/login`.
2. Backend validates against the shared in-memory user source and returns JWT access token + user payload.
3. Client stores token locally and loads the protected app shell.
4. On app boot, client calls `GET /api/auth/me` with token to restore session.
5. Missing/invalid token returns user to login.

## Seeded Dev Credentials

Development users available at server startup:

- `admin@officearcade.local` / `Admin@123` (role: `ADMIN`)
- `employee@officearcade.local` / `Employee@123` (role: `EMPLOYEE`)

Testing notes:

- Accounts created from the admin module can authenticate immediately.
- Admin password resets immediately affect future login attempts.
- Deactivated users are blocked by auth checks.
- User data is currently in-memory and resets when the backend restarts.

## Intentionally Not Implemented Yet

- PostgreSQL/Flyway persistence
- Department management and broader admin policy modules
- Room lifecycle or gameplay logic
- Respect/Karma systems
- Store/inventory systems
- Leaderboards
- WebSocket realtime gameplay infrastructure
- Signup/recovery/refresh-token production auth flows

## Run Locally

### 1) Start the Server

```bash
cd officearcade-server
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
cd officearcade-server
.\mvnw.cmd spring-boot:run
```

Server runs on `http://localhost:18180` by default.

Optional server port override:

```powershell
$env:OFFICEARCADE_SERVER_PORT="19180"
.\mvnw.cmd spring-boot:run
```

### 2) Start the Client (Web Dev Mode)

```bash
cd officearcade-client
npm install
npm run dev
```

Client runs on `http://localhost:5173`.

### 3) Start the Client (Tauri Desktop Mode)

```bash
cd officearcade-client
npm install
npm run tauri:dev
```

Optional API base override:

```bash
VITE_API_BASE_URL=http://localhost:18180
```

Windows PowerShell:

```powershell
$env:VITE_API_BASE_URL="http://localhost:18180"
```

## Branch Strategy

- `main` = stable/release-ready
- `dev` = integration branch
- `oa-ptXX-*` = feature branches for each project task

Each OA-PT is developed on its own task branch and merged manually into `dev`, then later into `main`.

## Next Step

`OA-PT04` will focus on core domain/schema and the next backend foundation layer beyond the temporary in-memory user model.
