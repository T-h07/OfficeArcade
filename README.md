# OfficeArcade

OfficeArcade is a workplace-friendly desktop gaming platform for short break-time multiplayer sessions inside a company environment. Employees use company-managed accounts to join or host game rooms, compete in quick games, earn progression, and interact through reputation systems like Respect and Karma, while admins control access, departments, play rules, and cooldown policies.

## Stack Overview

- Desktop shell: Tauri
- Frontend: React + TypeScript + Vite + Tailwind CSS
- Visual/game layer placeholder: PixiJS dependency included
- Backend: Spring Boot (Java 21)
- Persistence: PostgreSQL (Docker) + Flyway + Spring Data JPA
- Realtime (later): WebSocket

## Monorepo Structure

- `docs/` - concept, architecture, roadmap, and API notes
- `officearcade-client/` - Tauri + React + TypeScript desktop client
- `officearcade-server/` - Spring Boot backend
- `officearcade-shared/` - shared contracts/types/docs placeholders
- `assets/` - branding/cosmetics/avatars/mockups placeholders

## OA-PT04 Implemented Scope

- Dockerized local PostgreSQL environment (`docker-compose.yml`)
- Flyway migrations on backend startup
- Core persisted schema foundation:
  - `users`
  - `player_profiles`
  - `game_types`
- Seeded development data through Flyway:
  - admin + employee users
  - matching player profiles
  - base game type catalog (`CONNECT_FOUR`, `UNO`, `TRIVIA`)
- Auth and admin-user-management now use one persisted database source
- OA-PT02 and OA-PT03 flows preserved:
  - JWT login + `/api/auth/me`
  - role guards (`ADMIN`, `EMPLOYEE`)
  - admin users list/search/create/update/activate/deactivate/reset-password
  - last-active-admin safety protection

## Core Schema Overview

### `users`

- `id` (UUID, PK)
- `email` (unique)
- `display_name`
- `password_hash`
- `role` (`ADMIN` or `EMPLOYEE`)
- `enabled`
- `created_at`
- `updated_at`

### `player_profiles`

- `user_id` (UUID, PK/FK to `users`)
- `level`
- `xp`
- `respect_points`
- `karma_points`
- `games_played`
- `wins`
- `losses`
- `created_at`
- `updated_at`

### `game_types`

- `id` (UUID, PK)
- `code` (unique)
- `display_name`
- `enabled`
- `created_at`
- `updated_at`

## Seeded Dev Credentials

- `admin@officearcade.local` / `Admin@123` (role: `ADMIN`)
- `employee@officearcade.local` / `Employee@123` (role: `EMPLOYEE`)

Passwords are stored hashed in the database. Seed data is migration-driven.

## Docker Database Commands

### Start PostgreSQL

```bash
docker compose up -d officearcade-postgres
```

### View Container Status

```bash
docker compose ps
```

### Stop PostgreSQL (keep data volume)

```bash
docker compose stop officearcade-postgres
```

### Stop and remove container/network (keep volume)

```bash
docker compose down
```

### Full reset (remove DB volume/data)

```bash
docker compose down -v
```

## Run Locally

### 1) Start Database

```powershell
cd C:\Users\taulanth\Desktop\OfficeArcade
docker compose up -d officearcade-postgres
```

PostgreSQL is exposed at `localhost:55432` with:

- database: `officearcade`
- username: `officearcade`
- password: `officearcade`

### 2) Start Backend

```powershell
cd C:\Users\taulanth\Desktop\OfficeArcade\officearcade-server
.\mvnw.cmd spring-boot:run
```

Backend runs on `http://localhost:18180` by default.

Optional backend port override:

```powershell
$env:OFFICEARCADE_SERVER_PORT="19180"
.\mvnw.cmd spring-boot:run
```

### 3) Start Client (Web Dev Mode)

```powershell
cd C:\Users\taulanth\Desktop\OfficeArcade\officearcade-client
npm install
npm run dev
```

Client runs on `http://localhost:5173`.

### 4) Start Client (Tauri Desktop Mode)

```powershell
cd C:\Users\taulanth\Desktop\OfficeArcade\officearcade-client
npm install
npm run tauri:dev
```

Optional API base override for client:

```powershell
$env:VITE_API_BASE_URL="http://localhost:18180"
```

## Notes

- Flyway runs automatically on backend startup.
- Admin user management changes are persisted and survive backend restart.
- Deactivated users cannot authenticate.
- Password reset changes apply immediately and persist.

## Intentionally Not Implemented Yet

- Game room lifecycle and multiplayer/gameplay logic
- Cooldown policy enforcement
- Respect/Karma business workflows
- Store/inventory ownership flows
- Leaderboard logic
- WebSocket gameplay/realtime infrastructure
- Department/tag UI and related business modules

## Branch Strategy

- `main` = stable/release-ready
- `dev` = integration branch
- `oa-ptXX-*` = feature branches for each project task

Each OA-PT is developed on its own task branch and merged manually into `dev`, then later into `main`.

## Next Step

`OA-PT05` will build employee-facing dashboard/domain functionality on top of the persisted schema foundation created in OA-PT04.
