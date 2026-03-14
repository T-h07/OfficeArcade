CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(80) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    role VARCHAR(16) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'EMPLOYEE'))
);

CREATE TABLE player_profiles (
    user_id UUID PRIMARY KEY,
    level INTEGER NOT NULL DEFAULT 1,
    xp INTEGER NOT NULL DEFAULT 0,
    respect_points INTEGER NOT NULL DEFAULT 0,
    karma_points INTEGER NOT NULL DEFAULT 0,
    games_played INTEGER NOT NULL DEFAULT 0,
    wins INTEGER NOT NULL DEFAULT 0,
    losses INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_player_profiles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_player_profiles_level_non_negative CHECK (level >= 1),
    CONSTRAINT chk_player_profiles_xp_non_negative CHECK (xp >= 0),
    CONSTRAINT chk_player_profiles_respect_non_negative CHECK (respect_points >= 0),
    CONSTRAINT chk_player_profiles_karma_non_negative CHECK (karma_points >= 0),
    CONSTRAINT chk_player_profiles_games_played_non_negative CHECK (games_played >= 0),
    CONSTRAINT chk_player_profiles_wins_non_negative CHECK (wins >= 0),
    CONSTRAINT chk_player_profiles_losses_non_negative CHECK (losses >= 0)
);

CREATE TABLE game_types (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_role_enabled ON users(role, enabled);
