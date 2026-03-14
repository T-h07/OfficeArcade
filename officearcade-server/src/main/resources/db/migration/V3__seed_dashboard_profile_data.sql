UPDATE player_profiles
SET
    level = 7,
    xp = 1330,
    respect_points = 260,
    karma_points = 241,
    games_played = 55,
    wins = 34,
    losses = 21,
    updated_at = NOW()
WHERE user_id = '00000000-0000-0000-0000-000000000001';

UPDATE player_profiles
SET
    level = 5,
    xp = 860,
    respect_points = 142,
    karma_points = 118,
    games_played = 37,
    wins = 22,
    losses = 15,
    updated_at = NOW()
WHERE user_id = '00000000-0000-0000-0000-000000000002';
