CREATE TABLE player_play_limits (
    user_id UUID PRIMARY KEY,
    games_played_date DATE NOT NULL,
    games_played_today INTEGER NOT NULL DEFAULT 0,
    cooldown_until TIMESTAMPTZ NULL,
    last_completed_game_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_player_play_limits_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_player_play_limits_games_played_today_non_negative CHECK (games_played_today >= 0)
);

CREATE INDEX idx_player_play_limits_cooldown_until ON player_play_limits(cooldown_until);

INSERT INTO player_play_limits (
    user_id,
    games_played_date,
    games_played_today,
    cooldown_until,
    last_completed_game_at,
    created_at,
    updated_at
)
SELECT
    u.id,
    CURRENT_DATE,
    0,
    NULL,
    NULL,
    NOW(),
    NOW()
FROM users u
ON CONFLICT (user_id) DO NOTHING;

ALTER TABLE connect_four_games
    ADD COLUMN play_limits_applied BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE trivia_games
    ADD COLUMN play_limits_applied BOOLEAN NOT NULL DEFAULT FALSE;
