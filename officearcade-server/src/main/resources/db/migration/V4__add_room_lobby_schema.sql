CREATE TABLE rooms (
    id UUID PRIMARY KEY,
    host_user_id UUID NOT NULL,
    game_type_id UUID NOT NULL,
    room_name VARCHAR(80) NOT NULL,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(120),
    max_players INTEGER NOT NULL,
    rounds INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rooms_host_user FOREIGN KEY (host_user_id) REFERENCES users(id),
    CONSTRAINT fk_rooms_game_type FOREIGN KEY (game_type_id) REFERENCES game_types(id),
    CONSTRAINT chk_rooms_status CHECK (status IN ('OPEN', 'FULL', 'CLOSED')),
    CONSTRAINT chk_rooms_max_players CHECK (max_players BETWEEN 2 AND 8),
    CONSTRAINT chk_rooms_rounds CHECK (rounds BETWEEN 1 AND 10),
    CONSTRAINT chk_rooms_password_rules CHECK (
        (is_private = TRUE AND password_hash IS NOT NULL AND length(trim(password_hash)) > 0)
        OR
        (is_private = FALSE AND password_hash IS NULL)
    )
);

CREATE TABLE room_members (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    user_id UUID NOT NULL,
    member_role VARCHAR(16) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_room_members_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_room_members_role CHECK (member_role IN ('HOST', 'MEMBER')),
    CONSTRAINT uq_room_members_room_user UNIQUE (room_id, user_id),
    CONSTRAINT uq_room_members_user UNIQUE (user_id)
);

CREATE INDEX idx_rooms_status ON rooms(status);
CREATE INDEX idx_rooms_visibility ON rooms(is_private);
CREATE INDEX idx_rooms_game_type ON rooms(game_type_id);
CREATE INDEX idx_rooms_host_user ON rooms(host_user_id);
CREATE INDEX idx_room_members_room_joined ON room_members(room_id, joined_at);
