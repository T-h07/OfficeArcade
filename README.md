# OfficeArcade

OfficeArcade is a workplace-friendly desktop gaming platform for short break-time multiplayer sessions inside a company environment. Employees use company-managed accounts to join or host game rooms, compete in quick games, earn progression, and interact through reputation systems like Respect and Karma, while admins control access, departments, play rules, and cooldown policies.

## Stack Overview

- Desktop shell: Tauri
- Frontend: React + TypeScript + Vite + Tailwind CSS
- Visual/game layer placeholder: PixiJS dependency included
- Backend: Spring Boot (Java 21)
- Persistence: PostgreSQL (Docker) + Flyway + Spring Data JPA
- Realtime: Spring WebSocket + STOMP topics

## Monorepo Structure

- `docs/` - concept, architecture, roadmap, and API notes
- `officearcade-client/` - Tauri + React + TypeScript desktop client
- `officearcade-server/` - Spring Boot backend
- `officearcade-shared/` - shared contracts/types/docs placeholders
- `assets/` - branding/cosmetics/avatars/mockups placeholders

## OA-PT07 Implemented Scope

- Preserved OA-PT02 auth/session flow and OA-PT03 admin user management behavior
- Preserved OA-PT04 persisted foundation (`users`, `player_profiles`, `game_types`) and OA-PT05 employee dashboard
- Preserved OA-PT06 persisted room/lobby domain and APIs
- Added realtime backend infrastructure:
  - STOMP WebSocket endpoint: `/ws`
  - lobby topic: `/topic/lobby`
  - room topic pattern: `/topic/rooms/{roomId}`
  - JWT-authenticated STOMP connect/subscription handling
  - centralized lobby event publishing after successful room mutations
- Added realtime event propagation for room changes:
  - room created
  - room joined
  - room left
  - room closed (including host-leave close rule)
- Added realtime frontend infrastructure:
  - STOMP client connection lifecycle (connect/reconnect/degraded handling)
  - lobby topic subscription for live room-list sync
  - current-room topic subscription for live membership/status sync
  - graceful fallback: HTTP flows still work if realtime is unavailable
- Added Play page live-connection indicator and kept persisted server validations as source of truth

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

## Room/Lobby Realtime Notes (OA-PT07)

- Room and membership state is persisted in PostgreSQL.
- Default room list excludes `CLOSED` rooms.
- Lobby and current-room views now update live across sessions via STOMP topics.
- Client still refetches authoritative HTTP data after events/actions for consistency.
- If host leaves a room, the room is closed and all members are removed.
- WebSocket endpoint uses bearer token auth via STOMP CONNECT headers.

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
$env:VITE_REALTIME_WS_URL="ws://localhost:18180/ws"
```

## Intentionally Not Implemented Yet

- Room chat and live presence orchestration
- Multiplayer turn/gameplay execution
- First actual game logic/integration
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

`OA-PT08` will focus on first game integration on top of persisted + realtime room infrastructure.
