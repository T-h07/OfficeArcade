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

## OA-PT06 Implemented Scope

- Preserved OA-PT02 auth/session flow and OA-PT03 admin user management behavior
- Preserved OA-PT04 persisted foundation (`users`, `player_profiles`, `game_types`) and OA-PT05 employee dashboard
- Added persisted room/lobby domain (`rooms`, `room_members`) with Flyway migration `V4__add_room_lobby_schema.sql`
- Added lobby backend APIs (authenticated):
  - `GET /api/lobby/game-types`
  - `GET /api/lobby/rooms`
  - `GET /api/lobby/rooms/{roomId}`
  - `GET /api/lobby/my-room`
  - `POST /api/lobby/rooms`
  - `POST /api/lobby/rooms/{roomId}/join`
  - `POST /api/lobby/rooms/{roomId}/leave`
  - `POST /api/lobby/rooms/{roomId}/close`
- Added lobby/Play frontend module:
  - host room form (game type, rounds, max players, public/private + password)
  - room browser with join actions
  - private-room password prompt
  - current-room panel with members, leave, and host close action
  - loading/error/success handling with persisted refetch flow
- Added server-side room rules:
  - one active room membership per user
  - only enabled game types allowed
  - private room password validation
  - full-room join rejection
  - host leave rule: room closes and memberships are cleared

## Auth + Admin Scope (Current)

- JWT login and session restore
- `GET /api/auth/me`
- Role-guarded shell (`ADMIN`, `EMPLOYEE`)
- Admin user management:
  - list/search/filter
  - create/edit users
  - activate/deactivate
  - reset password
  - last-active-admin safety protection
- Auth and admin management share the same persisted user source

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

### `rooms`

- `id` (UUID, PK)
- `host_user_id` (FK to `users`)
- `game_type_id` (FK to `game_types`)
- `room_name`
- `is_private`
- `password_hash` (nullable for public rooms)
- `max_players`
- `rounds`
- `status` (`OPEN`, `FULL`, `CLOSED`)
- `created_at`
- `updated_at`

### `room_members`

- `id` (UUID, PK)
- `room_id` (FK to `rooms`)
- `user_id` (FK to `users`)
- `member_role` (`HOST` or `MEMBER`)
- `joined_at`
- uniqueness rules enforce:
  - no duplicate user in same room
  - one active room membership per user

## Seeded Dev Credentials

- `admin@officearcade.local` / `Admin@123` (role: `ADMIN`)
- `employee@officearcade.local` / `Employee@123` (role: `EMPLOYEE`)

Passwords are stored hashed in PostgreSQL. Seed data is migration-driven through Flyway.

## Seeded Dashboard Profile Notes

- Seeded admin and employee profiles include non-zero progression values for dashboard testing.
- Values are persisted in `player_profiles` and survive backend/client restarts.

## Room/Lobby Notes (OA-PT06)

- Room and membership state is persisted in PostgreSQL.
- Default room list excludes `CLOSED` rooms.
- Realtime updates are intentionally not implemented yet; client refresh/refetch is used after actions.
- If host leaves a room, the room is closed and all members are removed.

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

## Intentionally Not Implemented Yet

- WebSocket/STOMP realtime room synchronization
- Room chat and live presence orchestration
- Multiplayer turn/gameplay execution
- Respect/Karma business workflows
- Store/inventory ownership flows
- Leaderboard systems
- Department/tag modules

## Branch Strategy

- `main` = stable/release-ready
- `dev` = integration branch
- `oa-ptXX-*` = feature branches for each project task

Each OA-PT is developed on its own task branch and merged manually into `dev`, then later into `main`.

## Next Step

`OA-PT07` will focus on realtime room infrastructure and live lobby synchronization.
