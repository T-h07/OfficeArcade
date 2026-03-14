ALTER TABLE users
    ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN suspended_at TIMESTAMPTZ NULL,
    ADD COLUMN suspension_note VARCHAR(240) NULL,
    ADD COLUMN suspended_by_admin_id UUID NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_suspended_by_admin
        FOREIGN KEY (suspended_by_admin_id) REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX idx_users_suspended ON users(suspended);

ALTER TABLE post_match_challenges
    ADD COLUMN disputed_at TIMESTAMPTZ NULL,
    ADD COLUMN dispute_note VARCHAR(280) NULL,
    ADD COLUMN resolution_note VARCHAR(280) NULL,
    ADD COLUMN resolved_by_admin_id UUID NULL;

ALTER TABLE post_match_challenges
    ADD CONSTRAINT fk_post_match_challenges_resolved_by_admin
        FOREIGN KEY (resolved_by_admin_id) REFERENCES users(id) ON DELETE SET NULL;

ALTER TABLE post_match_challenges
    DROP CONSTRAINT IF EXISTS chk_post_match_challenges_status;

ALTER TABLE post_match_challenges
    ADD CONSTRAINT chk_post_match_challenges_status
        CHECK (status IN ('PENDING', 'COMPLETED_CONFIRMED', 'REJECTED', 'CANCELLED', 'EXPIRED', 'DISPUTED'));

CREATE INDEX idx_post_match_challenges_status_created_at
    ON post_match_challenges(status, created_at DESC);

CREATE TABLE moderation_reports (
    id UUID PRIMARY KEY,
    reporter_user_id UUID NOT NULL,
    reported_user_id UUID NOT NULL,
    category VARCHAR(64) NOT NULL,
    note VARCHAR(280) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    source_room_id UUID NULL,
    source_game_session_id UUID NULL,
    source_challenge_id UUID NULL,
    reviewed_by_admin_id UUID NULL,
    resolution_note VARCHAR(280) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_moderation_reports_reporter
        FOREIGN KEY (reporter_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_reports_reported
        FOREIGN KEY (reported_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_reports_room
        FOREIGN KEY (source_room_id) REFERENCES rooms(id) ON DELETE SET NULL,
    CONSTRAINT fk_moderation_reports_game_session
        FOREIGN KEY (source_game_session_id) REFERENCES connect_four_games(id) ON DELETE SET NULL,
    CONSTRAINT fk_moderation_reports_challenge
        FOREIGN KEY (source_challenge_id) REFERENCES post_match_challenges(id) ON DELETE SET NULL,
    CONSTRAINT fk_moderation_reports_reviewed_by
        FOREIGN KEY (reviewed_by_admin_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_moderation_reports_status
        CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT chk_moderation_reports_category
        CHECK (
            category IN (
                'UNSPORTSMANLIKE_BEHAVIOR',
                'CHALLENGE_DISPUTE',
                'HARASSMENT_OR_INAPPROPRIATE_BEHAVIOR',
                'ABUSE_OF_SYSTEM',
                'OTHER'
            )
        ),
    CONSTRAINT chk_moderation_reports_distinct_users
        CHECK (reporter_user_id <> reported_user_id)
);

CREATE INDEX idx_moderation_reports_status_created_at ON moderation_reports(status, created_at DESC);
CREATE INDEX idx_moderation_reports_reported_user ON moderation_reports(reported_user_id);
CREATE INDEX idx_moderation_reports_reporter_user ON moderation_reports(reporter_user_id);
CREATE INDEX idx_moderation_reports_category ON moderation_reports(category);

CREATE TABLE moderation_audit_log (
    id UUID PRIMARY KEY,
    admin_user_id UUID NOT NULL,
    target_user_id UUID NULL,
    action_type VARCHAR(64) NOT NULL,
    report_id UUID NULL,
    challenge_id UUID NULL,
    note VARCHAR(280) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_moderation_audit_admin
        FOREIGN KEY (admin_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_moderation_audit_target
        FOREIGN KEY (target_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_moderation_audit_report
        FOREIGN KEY (report_id) REFERENCES moderation_reports(id) ON DELETE SET NULL,
    CONSTRAINT fk_moderation_audit_challenge
        FOREIGN KEY (challenge_id) REFERENCES post_match_challenges(id) ON DELETE SET NULL
);

CREATE INDEX idx_moderation_audit_created_at ON moderation_audit_log(created_at DESC);
CREATE INDEX idx_moderation_audit_admin_user ON moderation_audit_log(admin_user_id);
CREATE INDEX idx_moderation_audit_target_user ON moderation_audit_log(target_user_id);
