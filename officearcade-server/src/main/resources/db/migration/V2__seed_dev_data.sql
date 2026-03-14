INSERT INTO users (id, email, display_name, password_hash, role, enabled, created_at, updated_at)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        'admin@officearcade.local',
        'Admin Operator',
        '$2a$10$GM59UomrBcaovJje8.1GF.6lzUykrtWM8VNIS8.jq.24SJh.NMTki',
        'ADMIN',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        'employee@officearcade.local',
        'Employee Player',
        '$2a$10$DD04Tt9kcUZBz8r6hBaJJOJWc0lVPsdmF5odJyW989f/hi6zp4M8W',
        'EMPLOYEE',
        TRUE,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO player_profiles (
    user_id,
    level,
    xp,
    respect_points,
    karma_points,
    games_played,
    wins,
    losses,
    created_at,
    updated_at
)
VALUES
    (
        '00000000-0000-0000-0000-000000000001',
        1,
        0,
        0,
        0,
        0,
        0,
        0,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000000002',
        1,
        0,
        0,
        0,
        0,
        0,
        0,
        NOW(),
        NOW()
    )
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO game_types (id, code, display_name, enabled, created_at, updated_at)
VALUES
    (
        '00000000-0000-0000-0000-000000010001',
        'CONNECT_FOUR',
        'Connect Four',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000010002',
        'UNO',
        'UNO',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        '00000000-0000-0000-0000-000000010003',
        'TRIVIA',
        'Trivia',
        TRUE,
        NOW(),
        NOW()
    )
ON CONFLICT (code) DO NOTHING;
