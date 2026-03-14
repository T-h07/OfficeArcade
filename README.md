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
- `officearcade-server/` - Spring Boot backend with auth/security foundation
- `officearcade-shared/` - shared contracts/types/docs placeholders for future PTs
- `assets/` - branding/cosmetics/avatars/mockups placeholders

## OA-PT02 Implemented Scope

- Spring Security + JWT authentication baseline
- Development-only seeded users (`ADMIN`, `EMPLOYEE`)
- Auth endpoints:
  - `POST /api/auth/login`
  - `GET /api/auth/me`
  - `GET /api/auth/role-check/admin` (admin-only role-gate probe)
- Existing `GET /api/health` retained as public
- Client auth-first flow:
  - login page
  - token persistence across reloads
  - startup session restore via `/api/auth/me`
  - logout flow
  - protected routing
  - role guards
- Minimal role-aware post-login app shell with placeholder pages:
  - Dashboard
  - Admin Overview (admin only)
  - Profile (employee only)
  - Settings

## Auth Flow Summary

1. User submits credentials to `POST /api/auth/login`.
2. Backend validates against seeded dev users and returns JWT access token + user payload.
3. Client stores token locally and loads the protected app shell.
4. On app boot, client calls `GET /api/auth/me` with token to restore session.
5. Missing/invalid token returns user to login.

## Seeded Dev Credentials

These are development-only credentials for OA-PT02:

- `admin@officearcade.local` / `Admin@123` (role: `ADMIN`)
- `employee@officearcade.local` / `Employee@123` (role: `EMPLOYEE`)

Note: this seeded model is intentionally temporary until later PTs introduce persistent identity/domain infrastructure.

## Intentionally Not Implemented Yet

- real admin user management workflows (planned for OA-PT03)
- department management
- game room lifecycle or gameplay logic
- respect/karma systems
- store/inventory systems
- leaderboard systems
- PostgreSQL/Flyway integration
- websocket/realtime gameplay infrastructure
- signup/recovery/refresh-token production auth flows

## Run Locally

### 1) Start the Server

```bash
cd officearcade-server
./mvnw spring-boot:run
```

Server runs on `http://localhost:8080`.

Windows PowerShell:

```powershell
cd officearcade-server
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

Optional env override (client):

```bash
VITE_API_BASE_URL=http://localhost:8080
```

On Windows PowerShell:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080"
```

## Branch Strategy

- `main` = stable/release-ready
- `dev` = integration branch
- `oa-ptXX-*` = feature branches for each project task

Each OA-PT is developed on its own task branch, merged manually into `dev`, then later into `main`.

## Next Step

`OA-PT03` will focus on Admin User Management foundations and management workflows on top of the OA-PT02 auth/role baseline.
