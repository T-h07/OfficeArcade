CREATE TABLE challenge_types (
    id UUID PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    description VARCHAR(240) NOT NULL,
    respect_reward_points INTEGER NOT NULL,
    karma_penalty_points INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_challenge_types_respect_non_negative CHECK (respect_reward_points >= 0),
    CONSTRAINT chk_challenge_types_karma_non_negative CHECK (karma_penalty_points >= 0)
);

CREATE TABLE post_match_challenges (
    id UUID PRIMARY KEY,
    source_game_session_id UUID NOT NULL,
    source_room_id UUID NOT NULL,
    challenge_type_id UUID NOT NULL,
    obligated_user_id UUID NOT NULL,
    beneficiary_user_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    respect_points_awarded INTEGER NOT NULL DEFAULT 0,
    karma_points_awarded INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_post_match_challenges_game_session
        FOREIGN KEY (source_game_session_id) REFERENCES connect_four_games(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_match_challenges_room
        FOREIGN KEY (source_room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_match_challenges_type
        FOREIGN KEY (challenge_type_id) REFERENCES challenge_types(id),
    CONSTRAINT fk_post_match_challenges_obligated_user
        FOREIGN KEY (obligated_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_match_challenges_beneficiary_user
        FOREIGN KEY (beneficiary_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_post_match_challenges_source_game UNIQUE (source_game_session_id),
    CONSTRAINT chk_post_match_challenges_status
        CHECK (status IN ('PENDING', 'COMPLETED_CONFIRMED', 'REJECTED', 'EXPIRED')),
    CONSTRAINT chk_post_match_challenges_non_negative_points
        CHECK (respect_points_awarded >= 0 AND karma_points_awarded >= 0),
    CONSTRAINT chk_post_match_challenges_users_different
        CHECK (obligated_user_id <> beneficiary_user_id)
);

CREATE INDEX idx_post_match_challenges_status ON post_match_challenges(status);
CREATE INDEX idx_post_match_challenges_obligated_user ON post_match_challenges(obligated_user_id);
CREATE INDEX idx_post_match_challenges_beneficiary_user ON post_match_challenges(beneficiary_user_id);
CREATE INDEX idx_post_match_challenges_created_at ON post_match_challenges(created_at DESC);

INSERT INTO challenge_types (
    id,
    code,
    display_name,
    description,
    respect_reward_points,
    karma_penalty_points,
    enabled,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-0000-0000-000000020001',
        'COFFEE_RUN',
        'Coffee Run',
        'Loser owes a simple coffee run during the next break window.',
        10,
        5,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000020002',
        'SNACK_TREAT',
        'Snack Treat',
        'Loser owes a small office-safe snack treat within the team area.',
        10,
        5,
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000020003',
        'NEXT_BREAK_FAVOR',
        'Next Break Favor',
        'Loser owes a light next-break favor such as grabbing water or coffee.',
        10,
        5,
        TRUE,
        NOW(),
        NOW()
    )
ON CONFLICT (code) DO NOTHING;
