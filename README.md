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

## OA-PT15 Implemented Scope

- Preserved OA-PT02 auth/session flow and OA-PT03 admin user management behavior
- Preserved OA-PT04 persisted foundation (`users`, `player_profiles`, `game_types`) and OA-PT05 employee dashboard
- Preserved OA-PT06 persisted room/lobby domain and OA-PT07 realtime lobby sync
- Preserved first playable game integration: **Connect Four**
  - server-authoritative game state and turn validation
  - room-linked game lifecycle (`WAITING`, `ACTIVE`, `FINISHED`)
  - start-match endpoint for host when exactly 2 room members are present
  - move endpoint with strict validation (membership, turn order, game status, column capacity)
  - win detection (horizontal, vertical, both diagonals)
  - draw detection when board is full
  - no moves accepted after game completion
- Added realtime Connect Four sync:
  - room game topic pattern: `/topic/games/connect-four/{roomId}`
  - live propagation for game started, move played, game finished, and game aborted
  - frontend game panel auto-resyncs from authoritative backend state on realtime events
- Added room/game safety rules for Connect Four:
  - Connect Four rooms require `maxPlayers = 2`
  - joining is blocked while an active Connect Four game exists
  - if a player leaves during an active Connect Four match, room closes and game is aborted cleanly
- Added Respect/Karma foundation tied to real completed matches:
  - persisted safe challenge catalog (`challenge_types`)
  - persisted post-match challenge records (`post_match_challenges`)
  - automatic challenge creation for completed non-draw Connect Four matches
  - loser becomes obligated player, winner becomes beneficiary/confirmer
  - beneficiary-only resolution flow:
    - confirm => obligated player gains Respect
    - reject/not fulfilled => obligated player gains Karma
  - duplicate resolution and unauthorized resolution protections
  - challenge state integrated into Connect Four result state and a dedicated client Challenges page
  - dashboard now surfaces pending/resolved challenge summaries
  - Karma does **not** alter gameplay fairness in OA-PT09
- Added Respect-powered store and inventory foundation:
  - persisted cosmetic catalog with category, rarity, price, enabled flag, and preview key
  - purchase flow with server-side Respect affordability checks and duplicate ownership blocking
  - persisted user ownership and equip state
  - one equipped item per category rule, with automatic category switch handling
  - Store page for catalog browsing and purchases
  - Inventory page for owned item management and equip/unequip actions
  - dashboard cosmetic summary (owned count, equipped count, equipped loadout)
  - purchase/equip state remains consistent after refresh/restart
- Added avatar loadout and profile customization presentation:
  - new authenticated profile aggregate endpoint: `GET /api/profile/me`
  - dedicated Profile / Customization page with category-based equip controls
  - layered avatar preview driven by persisted equipped cosmetics
  - deterministic layer ordering for visual loadout rendering
  - profile preview reacts immediately to equip/unequip and persists after refresh/restart
  - graceful fallback visual behavior for unknown/missing asset keys
- Added leaderboards and rankings:
  - authenticated leaderboard API with type metadata and multi-metric ranking views
  - supported leaderboard views:
    - Wins
    - Win Rate
    - Level
    - Respect
    - Karma (lower is better)
    - Games Played
  - deterministic rank ordering with stable tie-breakers
  - current-user rank context returned even when outside visible top list
  - compact profile-aware presentation support (profile frame/badge markers in ranking entries)
  - dedicated Leaderboards page with metric tabs, ranked rows, top-3 emphasis, and "Your Rank" summary
- Added second playable game integration: **Trivia Battle**
  - server-authoritative trivia state, question flow, round progression, and scoring
  - persisted trivia question bank (`trivia_questions`) seeded by Flyway
  - room-linked trivia session lifecycle (`WAITING`, `ACTIVE`, `FINISHED`)
  - host start rule for TRIVIA rooms with exactly 2 room members
  - answer submission endpoint with strict validation:
    - participant-only access
    - one answer per player per round
    - no invalid option indices
    - no submissions after round/match completion
  - live round and score synchronization via STOMP topic:
    - `/topic/games/trivia/{roomId}`
  - post-round result snapshots (correct answer + per-player correctness)
  - final winner/draw resolution and clean post-game state handling
  - TRIVIA room safety aligned with gameplay constraints:
    - TRIVIA rooms require `maxPlayers = 2`
    - joining is blocked while an active Trivia Battle is in progress
    - if a player leaves during an active Trivia match, room closes cleanly
- Added cooldown and daily play-limit enforcement foundation:
  - server-authoritative play eligibility with persisted per-user state
  - default policy:
    - daily completed game limit: `5`
    - cooldown after completed match: `90 minutes`
    - reset timezone: `Europe/Berlin`
  - daily reset logic based on server-side date in configured timezone
  - new authenticated play-limit API:
    - `GET /api/play-limits/me`
    - `GET /api/play-limits/me/eligibility`
  - blocked reasons returned as machine-readable states:
    - `ELIGIBLE`
    - `COOLDOWN_ACTIVE`
    - `DAILY_LIMIT_REACHED`
  - eligibility enforcement integrated into lobby + game flows:
    - blocked users cannot create/join playable rooms (`CONNECT_FOUR`, `TRIVIA`)
    - game start revalidates all participants
  - completed matches update per-user counters and cooldown timestamps exactly once
  - frontend now surfaces:
    - dashboard daily usage + cooldown summary
    - lobby blocked state with reason, remaining time, and reset visibility
    - countdown UX tied to server-authoritative rechecks
- Added moderation and safe workplace controls foundation:
  - authenticated player reporting flow via `POST /api/reports` with structured categories
  - report context support for room, game session, and challenge linkage
  - admin moderation control plane under `/api/admin/moderation/*`
    - report queue and report detail review (`OPEN`, `IN_REVIEW`, `RESOLVED`, `DISMISSED`)
    - admin report actions: dismiss, note only, suspend user, unsuspend user
    - challenge dispute queue + admin dispute resolution decisions
    - moderation audit trail endpoint for traceable admin actions
    - safe challenge policy read/update controls for enabled challenge types
  - challenge dispute path added:
    - participants can move pending challenges into `DISPUTED` review state
    - admin can resolve disputed challenges as completed, rejected, or neutral canceled
    - duplicate resolution protection remains enforced
  - persisted user suspension state with explicit blocked behavior:
    - suspended users can authenticate and receive account status
    - suspended users are blocked from normal `/api/**` app interactions (except `/api/auth/me`)
    - client shows a clear suspended account screen instead of normal app shell
  - new admin moderation page in client:
    - queue, detail actions, dispute review, policy toggles, and audit history
  - lobby + challenge surfaces now include practical report actions for users

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
- Moderation user safety controls:
  - suspend/unsuspend workflow for admins
  - suspended-user blocked-state handling in backend + frontend shell
- Auth and admin management share the same persisted user source

## Core Schema Overview

### `users`

- `id` (UUID, PK)
- `email` (unique)
- `display_name`
- `password_hash`
- `role` (`ADMIN` or `EMPLOYEE`)
- `enabled`
- `suspended`
- `suspended_at`
- `suspension_note`
- `suspended_by_admin_id` (nullable FK to `users`)
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

### `connect_four_games`

- `id` (UUID, PK)
- `room_id` (UUID, unique FK to `rooms`)
- `status` (`WAITING`, `ACTIVE`, `FINISHED`)
- `player_one_user_id` (FK to `users`)
- `player_two_user_id` (FK to `users`)
- `current_turn_user_id` (FK to `users`)
- `winner_user_id` (FK to `users`, nullable)
- `board_state` (42-char encoded 7x6 board)
- `move_count`
- `is_draw`
- `play_limits_applied`
- `created_at`
- `started_at`
- `ended_at`
- `updated_at`

### `trivia_questions`

- `id` (UUID, PK)
- `code` (unique)
- `prompt`
- `option_a`
- `option_b`
- `option_c`
- `option_d`
- `correct_option_index` (0-3)
- `category`
- `difficulty` (`EASY`, `MEDIUM`, `HARD`)
- `enabled`
- `created_at`
- `updated_at`

### `trivia_games`

- `id` (UUID, PK)
- `room_id` (UUID, unique FK to `rooms`)
- `status` (`WAITING`, `ACTIVE`, `FINISHED`)
- `player_one_user_id` (FK to `users`)
- `player_two_user_id` (FK to `users`)
- `current_round`
- `total_rounds`
- `question_sequence` (ordered list of selected question IDs for deterministic round flow)
- `current_question_id` (FK to `trivia_questions`)
- `last_resolved_round`
- `last_question_id` (FK to `trivia_questions`)
- `player_one_score`
- `player_two_score`
- `winner_user_id` (FK to `users`, nullable)
- `is_draw`
- `play_limits_applied`
- `created_at`
- `started_at`
- `ended_at`
- `updated_at`

### `player_play_limits`

- `user_id` (UUID, PK/FK to `users`)
- `games_played_date`
- `games_played_today`
- `cooldown_until`
- `last_completed_game_at`
- `created_at`
- `updated_at`

### `trivia_round_answers`

- `id` (UUID, PK)
- `game_id` (FK to `trivia_games`)
- `round_number`
- `question_id` (FK to `trivia_questions`)
- `user_id` (FK to `users`)
- `selected_option_index`
- `is_correct`
- `submitted_at`
- uniqueness rule enforces one answer per player per round: `(game_id, round_number, user_id)`

### `challenge_types`

- `id` (UUID, PK)
- `code` (unique safe category code)
- `display_name`
- `description`
- `respect_reward_points`
- `karma_penalty_points`
- `enabled`
- `created_at`
- `updated_at`

### `post_match_challenges`

- `id` (UUID, PK)
- `source_game_session_id` (unique FK to `connect_four_games`)
- `source_room_id` (FK to `rooms`)
- `challenge_type_id` (FK to `challenge_types`)
- `obligated_user_id` (FK to `users`)
- `beneficiary_user_id` (FK to `users`)
- `status` (`PENDING`, `DISPUTED`, `COMPLETED_CONFIRMED`, `REJECTED`, `CANCELLED`, `EXPIRED`)
- `respect_points_awarded`
- `karma_points_awarded`
- `created_at`
- `disputed_at`
- `dispute_note`
- `resolved_at`
- `resolution_note`
- `resolved_by_admin_id` (nullable FK to `users`)
- `updated_at`

### `moderation_reports`

- `id` (UUID, PK)
- `reporter_user_id` (FK to `users`)
- `reported_user_id` (FK to `users`)
- `category` (`UNSPORTSMANLIKE_BEHAVIOR`, `CHALLENGE_DISPUTE`, `HARASSMENT_OR_INAPPROPRIATE_BEHAVIOR`, `ABUSE_OF_SYSTEM`, `OTHER`)
- `note` (short optional report note)
- `status` (`OPEN`, `IN_REVIEW`, `RESOLVED`, `DISMISSED`)
- `source_room_id` (nullable FK to `rooms`)
- `source_game_session_id` (nullable FK to `connect_four_games`)
- `source_challenge_id` (nullable FK to `post_match_challenges`)
- `reviewed_by_admin_id` (nullable FK to `users`)
- `resolution_note`
- `created_at`
- `updated_at`

### `moderation_audit_log`

- `id` (UUID, PK)
- `admin_user_id` (FK to `users`)
- `target_user_id` (nullable FK to `users`)
- `action_type` (moderation action enum value)
- `report_id` (nullable FK to `moderation_reports`)
- `challenge_id` (nullable FK to `post_match_challenges`)
- `note`
- `created_at`

### `cosmetic_items`

- `id` (UUID, PK)
- `code` (unique)
- `display_name`
- `description`
- `category` (`HAT`, `GLASSES`, `OUTFIT`, `PROFILE_FRAME`, `BADGE`, `ACCESSORY`)
- `rarity` (`COMMON`, `RARE`, `EPIC`)
- `price_respect`
- `preview_asset_key`
- `enabled`
- `created_at`
- `updated_at`

### `user_owned_cosmetics`

- `id` (UUID, PK)
- `user_id` (FK to `users`)
- `cosmetic_item_id` (FK to `cosmetic_items`)
- `acquired_at`
- unique ownership rule: `(user_id, cosmetic_item_id)`

### `user_equipped_cosmetics`

- `id` (UUID, PK)
- `user_id` (FK to `users`)
- `cosmetic_item_id` (FK to `cosmetic_items`)
- `category`
- `equipped_at`
- uniqueness rules enforce:
  - one equipped item per category per user: `(user_id, category)`
  - one equip record per item per user: `(user_id, cosmetic_item_id)`

## Seeded Dev Credentials

- `admin@officearcade.local` / `Admin@123` (role: `ADMIN`)
- `employee@officearcade.local` / `Employee@123` (role: `EMPLOYEE`)

Passwords are stored hashed in PostgreSQL. Seed data is migration-driven through Flyway.

## Seeded Dashboard Profile Notes

- Seeded admin and employee profiles include non-zero progression values for dashboard testing.
- Values are persisted in `player_profiles` and survive backend/client restarts.

## Store / Inventory Seed Notes (OA-PT10)

- Flyway seeds a starter cosmetics catalog with office-safe items across multiple categories/rarities.
- The seeded employee profile is elevated to a usable Respect balance baseline for purchase testing.
- Respect remains the only purchase currency in OA-PT10; Karma is not spendable.

## Profile / Loadout Seed Notes (OA-PT11)

- Flyway seeds a default owned/equipped cosmetic loadout for the seeded employee account to make profile rendering immediately testable.
- The profile aggregate endpoint returns:
  - owned cosmetics
  - equipped cosmetics
  - pre-ordered avatar layer metadata
- Layering order is deterministic:
  - `BASE_BODY`, `OUTFIT`, `ACCESSORY`, `GLASSES`, `HAT`, `PROFILE_FRAME`, `BADGE`

## Leaderboard Rules (OA-PT12)

- Ranking data is sourced from persisted `users` + `player_profiles` (+ equipped profile markers).
- Tie handling is deterministic and stable through explicit tie-break ordering.
- Win Rate leaderboard applies a minimum completed-match threshold (`wins + losses >= 5`).
- Karma leaderboard is intentionally ordered ascending (lower karma ranks higher).
- API exposes current-user ranking context even if the user is outside the visible top list.

## Realtime + Game + Reputation + Store + Profile + Leaderboard + Play-Limits + Moderation Notes (OA-PT15)

- Room and membership state is persisted in PostgreSQL.
- Default room list excludes `CLOSED` rooms.
- Lobby and current-room views now update live across sessions via STOMP topics.
- Client still refetches authoritative HTTP data after events/actions for consistency.
- If host leaves a room, the room is closed and all members are removed.
- WebSocket endpoint uses bearer token auth via STOMP CONNECT headers.
- Connect Four rooms can transition into active gameplay once host starts with 2 players present.
- Connect Four board state is synchronized live across clients via room-specific game topic events.
- Gameplay correctness is server-authoritative; frontend never decides official outcomes.
- Trivia rooms can transition into active gameplay once host starts with 2 players present.
- Trivia question flow, answer state, and score updates are synchronized live across clients via:
  - `/topic/games/trivia/{roomId}`
- Trivia correctness is server-authoritative; frontend never decides official scoring/outcomes.
- Completed non-draw Connect Four matches create one safe post-match challenge record.
- Challenges are resolved manually through confirm/reject actions by the beneficiary only.
- Respect/Karma updates are persisted on player profiles and reflected in dashboard + challenge history views.
- Store catalog and inventory data are persisted and API-driven.
- Purchase/equip correctness is server-authoritative; client state is response/refetch driven.
- Profile avatar preview uses `preview_asset_key` conventions from cosmetic catalog rows.
  - Example key format: `category.variant-name` (e.g., `hat.classic-cap`, `frame.neon`)
  - Frontend maps keys to layered placeholder render presets with fallback visuals.
- Leaderboards use normal authenticated HTTP fetches (no dedicated realtime ranking channel in OA-PT12).
- Play-limit eligibility is server-authoritative and persisted in `player_play_limits`.
- Daily counts reset by server date in timezone `Europe/Berlin`.
- Default PT14 policy is `5` completed games/day and `90` minutes cooldown per completed match.
- Play-limit state survives refresh, app restart, and backend restart.
- Moderation reports and moderation audit actions are persisted in PostgreSQL.
- Suspended users are explicitly blocked from normal app APIs while suspension is active.
- Challenge disputes can be escalated to `DISPUTED` and admin-resolved without duplicate point application.
- Challenge safety policy toggles operate through persisted `challenge_types.enabled` controls.

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
- Additional game integrations beyond Connect Four and Trivia (UNO remains placeholder)
- Department/tag modules
- Advanced avatar rendering/customization scene tooling
- Public social profile directory/sharing flows
- Seasonal resets and reward payout systems

## Branch Strategy

- `main` = stable/release-ready
- `dev` = integration branch
- `oa-ptXX-*` = feature branches for each project task

Each OA-PT is developed on its own task branch and merged manually into `dev`, then later into `main`.

## Next Step

`OA-PT16+` can expand moderation notifications, departmental/admin grouping controls, richer policy administration UX, and broader gameplay/social modules on top of the OA-PT15 safety baseline.
