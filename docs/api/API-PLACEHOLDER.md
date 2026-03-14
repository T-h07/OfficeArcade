# API Placeholder

This document is a section scaffold only. Endpoint definitions are intentionally not specified yet.

## Authentication

- `POST /api/auth/login` (implemented)
- `GET /api/auth/me` (implemented, JWT required)

## Users

- Admin user management endpoints are implemented in OA-PT03/OA-PT04:
  - `GET /api/admin/users`
  - `GET /api/admin/users/{id}`
  - `POST /api/admin/users`
  - `PUT /api/admin/users/{id}`
  - `POST /api/admin/users/{id}/activate`
  - `POST /api/admin/users/{id}/deactivate`
  - `POST /api/admin/users/{id}/reset-password`

Persistence note:
- OA-PT04 migrated auth and admin user management to PostgreSQL + Flyway.

## Departments

## Rooms

## Games

## Respect/Karma

## Store/Inventory

## Leaderboards
