# OfficeArcade

OfficeArcade is a workplace-friendly desktop gaming platform for short break-time multiplayer sessions inside a company environment. Employees use company-managed accounts to join or host game rooms, compete in quick games, earn progression, and interact through reputation systems like Respect and Karma, while admins control access, departments, play rules, and cooldown policies.

## Stack Overview

- Desktop shell: Tauri
- Frontend: React + TypeScript + Vite + Tailwind CSS
- Visual/game layer placeholder: PixiJS dependency added (no gameplay implementation in OA-PT01)
- Backend: Spring Boot (Java 21)
- Persistence (later): PostgreSQL
- Realtime (later): WebSocket

## Monorepo Structure

- `docs/` - concept, architecture, roadmap, and API placeholders
- `officearcade-client/` - Tauri + React + TS + Vite + Tailwind desktop client foundation
- `officearcade-server/` - Spring Boot server foundation with health API
- `officearcade-shared/` - placeholders for shared contracts, shared types, and cross-layer docs
- `assets/` - branding/cosmetics/avatars/mockups placeholders

## OA-PT01 Implemented Scope

- Runnable Spring Boot server baseline
- `GET /api/health` endpoint with status/application/profile/timestamp payload
- CORS enabled for local frontend and Tauri dev origins
- Runnable React + TypeScript + Vite + Tailwind client foundation
- Tauri desktop shell configuration and minimal Rust entrypoint
- Foundation landing page with client/server status UI
- Client call to backend health endpoint with loading/success/offline handling
- Environment-based API base URL (`VITE_API_BASE_URL`)

## Intentionally Not Implemented Yet

- authentication and authorization logic
- admin features and user management
- departments, room flows, multiplayer logic
- gameplay/PixiJS scene implementation
- respect/karma, leaderboard, store/inventory logic
- PostgreSQL integration and production schema
- WebSocket/realtime infrastructure

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

`OA-PT02` will focus on app shell/navigation and role-oriented structure (Admin and Employee paths) without introducing full business features yet.
