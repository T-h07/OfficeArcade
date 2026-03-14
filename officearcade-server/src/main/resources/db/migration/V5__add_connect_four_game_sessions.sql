CREATE TABLE connect_four_games (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL,
    player_one_user_id UUID,
    player_two_user_id UUID,
    current_turn_user_id UUID,
    winner_user_id UUID,
    board_state VARCHAR(42) NOT NULL,
    move_count INTEGER NOT NULL DEFAULT 0,
    is_draw BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ NULL,
    ended_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_connect_four_games_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_connect_four_games_player_one FOREIGN KEY (player_one_user_id) REFERENCES users(id),
    CONSTRAINT fk_connect_four_games_player_two FOREIGN KEY (player_two_user_id) REFERENCES users(id),
    CONSTRAINT fk_connect_four_games_current_turn FOREIGN KEY (current_turn_user_id) REFERENCES users(id),
    CONSTRAINT fk_connect_four_games_winner FOREIGN KEY (winner_user_id) REFERENCES users(id),
    CONSTRAINT chk_connect_four_games_status CHECK (status IN ('WAITING', 'ACTIVE', 'FINISHED')),
    CONSTRAINT chk_connect_four_games_board_len CHECK (char_length(board_state) = 42),
    CONSTRAINT chk_connect_four_games_move_count CHECK (move_count BETWEEN 0 AND 42)
);

CREATE INDEX idx_connect_four_games_status ON connect_four_games(status);
