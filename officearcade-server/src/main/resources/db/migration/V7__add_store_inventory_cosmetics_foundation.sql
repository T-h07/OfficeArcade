CREATE TABLE cosmetic_items (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    description VARCHAR(240) NOT NULL,
    category VARCHAR(32) NOT NULL,
    rarity VARCHAR(16) NOT NULL,
    price_respect INTEGER NOT NULL,
    preview_asset_key VARCHAR(120) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_cosmetic_items_category CHECK (
        category IN ('HAT', 'GLASSES', 'OUTFIT', 'PROFILE_FRAME', 'BADGE', 'ACCESSORY')
    ),
    CONSTRAINT chk_cosmetic_items_rarity CHECK (
        rarity IN ('COMMON', 'RARE', 'EPIC')
    ),
    CONSTRAINT chk_cosmetic_items_price_non_negative CHECK (price_respect >= 0)
);

CREATE TABLE user_owned_cosmetics (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    cosmetic_item_id UUID NOT NULL,
    acquired_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_owned_cosmetics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_owned_cosmetics_item FOREIGN KEY (cosmetic_item_id) REFERENCES cosmetic_items(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_owned_cosmetics_user_item UNIQUE (user_id, cosmetic_item_id)
);

CREATE TABLE user_equipped_cosmetics (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    cosmetic_item_id UUID NOT NULL,
    category VARCHAR(32) NOT NULL,
    equipped_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_user_equipped_cosmetics_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_equipped_cosmetics_item FOREIGN KEY (cosmetic_item_id) REFERENCES cosmetic_items(id) ON DELETE CASCADE,
    CONSTRAINT chk_user_equipped_cosmetics_category CHECK (
        category IN ('HAT', 'GLASSES', 'OUTFIT', 'PROFILE_FRAME', 'BADGE', 'ACCESSORY')
    ),
    CONSTRAINT uq_user_equipped_cosmetics_user_category UNIQUE (user_id, category),
    CONSTRAINT uq_user_equipped_cosmetics_user_item UNIQUE (user_id, cosmetic_item_id)
);

CREATE INDEX idx_cosmetic_items_category_rarity ON cosmetic_items(category, rarity);
CREATE INDEX idx_cosmetic_items_enabled ON cosmetic_items(enabled);
CREATE INDEX idx_user_owned_cosmetics_user ON user_owned_cosmetics(user_id, acquired_at DESC);
CREATE INDEX idx_user_equipped_cosmetics_user ON user_equipped_cosmetics(user_id, equipped_at DESC);

INSERT INTO cosmetic_items (
    id, code, display_name, description, category, rarity, price_respect, preview_asset_key, enabled, created_at, updated_at
)
VALUES
    ('00000000-0000-0000-0000-000000030001', 'CLASSIC_CAP', 'Classic Cap', 'Clean office-friendly cap with neutral styling.', 'HAT', 'COMMON', 25, 'hat.classic-cap', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030002', 'NIGHTSHIFT_BEANIE', 'Nightshift Beanie', 'Comfort beanie for late-break match vibes.', 'HAT', 'RARE', 45, 'hat.nightshift-beanie', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030003', 'DESKFRAME_GLASSES', 'Deskframe Glasses', 'Minimal office glasses with crisp lines.', 'GLASSES', 'COMMON', 20, 'glasses.deskframe', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030004', 'FOCUS_MODE_SHADES', 'Focus Mode Shades', 'Sharp shades for confident game sessions.', 'GLASSES', 'RARE', 55, 'glasses.focus-mode', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030005', 'HOODIE_CASUAL', 'Casual Hoodie', 'Relaxed break-time hoodie outfit.', 'OUTFIT', 'COMMON', 35, 'outfit.hoodie-casual', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030006', 'BLAZER_PRO', 'Pro Blazer', 'Professional blazer-style outfit for ranked pride.', 'OUTFIT', 'EPIC', 90, 'outfit.blazer-pro', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030007', 'FRAME_NEON', 'Neon Frame', 'Neon profile frame with clean contrast.', 'PROFILE_FRAME', 'RARE', 50, 'frame.neon', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030008', 'FRAME_EXECUTIVE', 'Executive Frame', 'Sleek frame for polished profile presence.', 'PROFILE_FRAME', 'EPIC', 85, 'frame.executive', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030009', 'BADGE_COFFEE_CLUB', 'Coffee Club Badge', 'Harmless badge for coffee-run legends.', 'BADGE', 'COMMON', 15, 'badge.coffee-club', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030010', 'BADGE_TEAM_PLAYER', 'Team Player Badge', 'Recognition badge for positive office play.', 'BADGE', 'RARE', 40, 'badge.team-player', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030011', 'ACCESSORY_STRIPE_TIE', 'Stripe Tie', 'Smart tie accessory with understated flair.', 'ACCESSORY', 'COMMON', 30, 'accessory.stripe-tie', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000030012', 'ACCESSORY_GOLD_PIN', 'Gold Pin', 'Small premium pin accessory for profile identity.', 'ACCESSORY', 'EPIC', 95, 'accessory.gold-pin', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

UPDATE player_profiles
SET respect_points = GREATEST(respect_points, 220),
    updated_at = NOW()
WHERE user_id = '00000000-0000-0000-0000-000000000002';
