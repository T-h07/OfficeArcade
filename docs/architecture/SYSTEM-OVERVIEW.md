# System Overview (Draft)

## Platform Shape

OfficeArcade is planned as a company-internal desktop platform served from one central LAN-hosted backend.

## Core Technology Direction

- Desktop client: Tauri + React + TypeScript + Tailwind + PixiJS
- Backend: Spring Boot
- Persistence: PostgreSQL (Dockerized local development)
- Schema migrations: Flyway
- Realtime: WebSocket

## Deployment Model

- One central company LAN server hosts backend APIs, realtime services, and database connectivity.
- Desktop clients connect from employee/admin machines on the same managed network environment.

## User Experience Model

- Employee UI: join/host rooms, play quick sessions, and track progression/reputation.
- Admin UI: manage access, organizational controls, and gameplay policy settings.

Role-based UI boundaries will be enforced at both frontend and backend layers in later phases.

## Current Persistence Baseline (OA-PT04)

- Core database tables:
  - `users`
  - `player_profiles`
  - `game_types`
- Auth and admin user-management now operate against persisted PostgreSQL data.
- Development seed data is migration-driven for reproducible local environments.
