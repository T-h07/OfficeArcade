CREATE TABLE uno_games (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL,
    player_order_state TEXT NOT NULL,
    current_turn_index INTEGER NOT NULL DEFAULT 0,
    direction INTEGER NOT NULL DEFAULT 1,
    current_color VARCHAR(16),
    draw_pile_state TEXT NOT NULL,
    discard_pile_state TEXT NOT NULL,
    hands_state TEXT NOT NULL,
    winner_user_id UUID,
    move_count INTEGER NOT NULL DEFAULT 0,
    play_limits_applied BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_uno_games_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_uno_games_winner FOREIGN KEY (winner_user_id) REFERENCES users(id),
    CONSTRAINT chk_uno_games_status CHECK (status IN ('WAITING', 'ACTIVE', 'FINISHED')),
    CONSTRAINT chk_uno_games_turn_index CHECK (current_turn_index >= 0),
    CONSTRAINT chk_uno_games_direction CHECK (direction IN (-1, 1)),
    CONSTRAINT chk_uno_games_current_color CHECK (
        current_color IS NULL OR current_color IN ('RED', 'YELLOW', 'GREEN', 'BLUE')
    )
);

CREATE INDEX idx_uno_games_status ON uno_games(status);
