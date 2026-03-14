CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(140) NOT NULL,
    message VARCHAR(320) NOT NULL,
    navigation_path VARCHAR(200) NULL,
    source_room_id UUID NULL,
    source_game_session_id UUID NULL,
    source_challenge_id UUID NULL,
    source_store_item_id UUID NULL,
    source_report_id UUID NULL,
    event_key VARCHAR(160) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_notifications_user
        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_room
        FOREIGN KEY (source_room_id) REFERENCES rooms(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_game
        FOREIGN KEY (source_game_session_id) REFERENCES connect_four_games(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_challenge
        FOREIGN KEY (source_challenge_id) REFERENCES post_match_challenges(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_store_item
        FOREIGN KEY (source_store_item_id) REFERENCES cosmetic_items(id) ON DELETE SET NULL,
    CONSTRAINT fk_notifications_report
        FOREIGN KEY (source_report_id) REFERENCES moderation_reports(id) ON DELETE SET NULL,
    CONSTRAINT chk_notifications_type
        CHECK (
            type IN (
                'CHALLENGE_CREATED',
                'CHALLENGE_CONFIRMED',
                'CHALLENGE_REJECTED',
                'CHALLENGE_DISPUTE_RESOLVED',
                'RESPECT_GAINED',
                'KARMA_APPLIED',
                'STORE_PURCHASE_SUCCESS',
                'ITEM_EQUIPPED',
                'MODERATION_STATUS_UPDATE',
                'REPORT_STATUS_UPDATE',
                'GAME_RESULT'
            )
        )
);

CREATE INDEX idx_notifications_user_created_at
    ON notifications(user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, read_at);
CREATE INDEX idx_notifications_user_type
    ON notifications(user_id, type);

CREATE UNIQUE INDEX uq_notifications_user_event_key
    ON notifications(user_id, event_key)
    WHERE event_key IS NOT NULL;
