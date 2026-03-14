INSERT INTO user_owned_cosmetics (id, user_id, cosmetic_item_id, acquired_at)
SELECT
    '00000000-0000-0000-0000-000000040001',
    '00000000-0000-0000-0000-000000000002',
    id,
    NOW()
FROM cosmetic_items
WHERE code = 'HOODIE_CASUAL'
ON CONFLICT (user_id, cosmetic_item_id) DO NOTHING;

INSERT INTO user_owned_cosmetics (id, user_id, cosmetic_item_id, acquired_at)
SELECT
    '00000000-0000-0000-0000-000000040002',
    '00000000-0000-0000-0000-000000000002',
    id,
    NOW()
FROM cosmetic_items
WHERE code = 'CLASSIC_CAP'
ON CONFLICT (user_id, cosmetic_item_id) DO NOTHING;

INSERT INTO user_owned_cosmetics (id, user_id, cosmetic_item_id, acquired_at)
SELECT
    '00000000-0000-0000-0000-000000040003',
    '00000000-0000-0000-0000-000000000002',
    id,
    NOW()
FROM cosmetic_items
WHERE code = 'FRAME_NEON'
ON CONFLICT (user_id, cosmetic_item_id) DO NOTHING;

INSERT INTO user_owned_cosmetics (id, user_id, cosmetic_item_id, acquired_at)
SELECT
    '00000000-0000-0000-0000-000000040004',
    '00000000-0000-0000-0000-000000000002',
    id,
    NOW()
FROM cosmetic_items
WHERE code = 'BADGE_COFFEE_CLUB'
ON CONFLICT (user_id, cosmetic_item_id) DO NOTHING;

DELETE FROM user_equipped_cosmetics
WHERE user_id = '00000000-0000-0000-0000-000000000002'
  AND category IN ('OUTFIT', 'HAT', 'PROFILE_FRAME', 'BADGE');

INSERT INTO user_equipped_cosmetics (id, user_id, cosmetic_item_id, category, equipped_at)
SELECT
    '00000000-0000-0000-0000-000000050001',
    '00000000-0000-0000-0000-000000000002',
    id,
    'OUTFIT',
    NOW()
FROM cosmetic_items
WHERE code = 'HOODIE_CASUAL'
ON CONFLICT (user_id, category) DO UPDATE
SET cosmetic_item_id = EXCLUDED.cosmetic_item_id,
    equipped_at = NOW();

INSERT INTO user_equipped_cosmetics (id, user_id, cosmetic_item_id, category, equipped_at)
SELECT
    '00000000-0000-0000-0000-000000050002',
    '00000000-0000-0000-0000-000000000002',
    id,
    'HAT',
    NOW()
FROM cosmetic_items
WHERE code = 'CLASSIC_CAP'
ON CONFLICT (user_id, category) DO UPDATE
SET cosmetic_item_id = EXCLUDED.cosmetic_item_id,
    equipped_at = NOW();

INSERT INTO user_equipped_cosmetics (id, user_id, cosmetic_item_id, category, equipped_at)
SELECT
    '00000000-0000-0000-0000-000000050003',
    '00000000-0000-0000-0000-000000000002',
    id,
    'PROFILE_FRAME',
    NOW()
FROM cosmetic_items
WHERE code = 'FRAME_NEON'
ON CONFLICT (user_id, category) DO UPDATE
SET cosmetic_item_id = EXCLUDED.cosmetic_item_id,
    equipped_at = NOW();

INSERT INTO user_equipped_cosmetics (id, user_id, cosmetic_item_id, category, equipped_at)
SELECT
    '00000000-0000-0000-0000-000000050004',
    '00000000-0000-0000-0000-000000000002',
    id,
    'BADGE',
    NOW()
FROM cosmetic_items
WHERE code = 'BADGE_COFFEE_CLUB'
ON CONFLICT (user_id, category) DO UPDATE
SET cosmetic_item_id = EXCLUDED.cosmetic_item_id,
    equipped_at = NOW();
