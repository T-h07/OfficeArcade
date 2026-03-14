CREATE TABLE trivia_questions (
    id UUID PRIMARY KEY,
    code VARCHAR(80) NOT NULL UNIQUE,
    prompt VARCHAR(320) NOT NULL,
    option_a VARCHAR(180) NOT NULL,
    option_b VARCHAR(180) NOT NULL,
    option_c VARCHAR(180) NOT NULL,
    option_d VARCHAR(180) NOT NULL,
    correct_option_index INTEGER NOT NULL,
    category VARCHAR(60) NOT NULL,
    difficulty VARCHAR(16) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_trivia_questions_correct_option CHECK (correct_option_index BETWEEN 0 AND 3),
    CONSTRAINT chk_trivia_questions_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

CREATE TABLE trivia_games (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL,
    player_one_user_id UUID,
    player_two_user_id UUID,
    current_round INTEGER NOT NULL DEFAULT 0,
    total_rounds INTEGER NOT NULL,
    question_sequence VARCHAR(512) NOT NULL,
    current_question_id UUID,
    last_resolved_round INTEGER,
    last_question_id UUID,
    player_one_score INTEGER NOT NULL DEFAULT 0,
    player_two_score INTEGER NOT NULL DEFAULT 0,
    winner_user_id UUID,
    is_draw BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_trivia_games_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_trivia_games_player_one FOREIGN KEY (player_one_user_id) REFERENCES users(id),
    CONSTRAINT fk_trivia_games_player_two FOREIGN KEY (player_two_user_id) REFERENCES users(id),
    CONSTRAINT fk_trivia_games_current_question FOREIGN KEY (current_question_id) REFERENCES trivia_questions(id),
    CONSTRAINT fk_trivia_games_last_question FOREIGN KEY (last_question_id) REFERENCES trivia_questions(id),
    CONSTRAINT fk_trivia_games_winner FOREIGN KEY (winner_user_id) REFERENCES users(id),
    CONSTRAINT chk_trivia_games_status CHECK (status IN ('WAITING', 'ACTIVE', 'FINISHED')),
    CONSTRAINT chk_trivia_games_rounds CHECK (total_rounds BETWEEN 1 AND 10),
    CONSTRAINT chk_trivia_games_current_round CHECK (current_round BETWEEN 0 AND 10),
    CONSTRAINT chk_trivia_games_scores_non_negative CHECK (player_one_score >= 0 AND player_two_score >= 0),
    CONSTRAINT chk_trivia_games_question_sequence CHECK (length(trim(question_sequence)) > 0)
);

CREATE TABLE trivia_round_answers (
    id UUID PRIMARY KEY,
    game_id UUID NOT NULL,
    round_number INTEGER NOT NULL,
    question_id UUID NOT NULL,
    user_id UUID NOT NULL,
    selected_option_index INTEGER NOT NULL,
    is_correct BOOLEAN NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_trivia_round_answers_game FOREIGN KEY (game_id) REFERENCES trivia_games(id) ON DELETE CASCADE,
    CONSTRAINT fk_trivia_round_answers_question FOREIGN KEY (question_id) REFERENCES trivia_questions(id),
    CONSTRAINT fk_trivia_round_answers_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uq_trivia_round_answers_round_user UNIQUE (game_id, round_number, user_id),
    CONSTRAINT chk_trivia_round_answers_round_number CHECK (round_number >= 1),
    CONSTRAINT chk_trivia_round_answers_selected_option CHECK (selected_option_index BETWEEN 0 AND 3)
);

CREATE INDEX idx_trivia_questions_enabled ON trivia_questions(enabled);
CREATE INDEX idx_trivia_questions_category ON trivia_questions(category);
CREATE INDEX idx_trivia_games_status ON trivia_games(status);
CREATE INDEX idx_trivia_round_answers_game_round ON trivia_round_answers(game_id, round_number);
CREATE INDEX idx_trivia_round_answers_user ON trivia_round_answers(user_id);

INSERT INTO trivia_questions (
    id, code, prompt, option_a, option_b, option_c, option_d, correct_option_index, category, difficulty, enabled, created_at, updated_at
)
VALUES
    ('00000000-0000-0000-0000-000000060001', 'TRIVIA_GK_001', 'What does CPU stand for?', 'Central Processing Unit', 'Core Processing Utility', 'Central Program Upload', 'Control Program Unit', 0, 'Tech', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060002', 'TRIVIA_GK_002', 'Which planet is known as the Red Planet?', 'Mercury', 'Mars', 'Venus', 'Jupiter', 1, 'General Knowledge', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060003', 'TRIVIA_GK_003', 'How many minutes are in 2 hours?', '100', '110', '120', '130', 2, 'General Knowledge', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060004', 'TRIVIA_GK_004', 'Which file extension is commonly used for Java source files?', '.js', '.java', '.jar', '.class', 1, 'Tech', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060005', 'TRIVIA_GK_005', 'What is the capital city of Germany?', 'Vienna', 'Hamburg', 'Berlin', 'Munich', 2, 'General Knowledge', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060006', 'TRIVIA_GK_006', 'Which keyboard shortcut is commonly used to copy text on Windows?', 'Ctrl + C', 'Ctrl + V', 'Ctrl + Z', 'Ctrl + X', 0, 'Office Culture', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060007', 'TRIVIA_GK_007', 'Which data structure works on Last In, First Out?', 'Queue', 'Tree', 'Stack', 'Graph', 2, 'Tech', 'MEDIUM', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060008', 'TRIVIA_GK_008', 'How many sides does a hexagon have?', '5', '6', '7', '8', 1, 'General Knowledge', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060009', 'TRIVIA_GK_009', 'What does HTTP stand for?', 'HyperText Transfer Protocol', 'High Transfer Text Program', 'HyperTerminal Text Process', 'Host Transfer Tunnel Protocol', 0, 'Tech', 'MEDIUM', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060010', 'TRIVIA_GK_010', 'In office meetings, what does ETA usually mean?', 'Estimated Time of Arrival', 'Event Time Agenda', 'Estimated Team Assignment', 'End Task Approval', 0, 'Office Culture', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060011', 'TRIVIA_GK_011', 'Which number is a prime number?', '21', '27', '29', '33', 2, 'General Knowledge', 'MEDIUM', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060012', 'TRIVIA_GK_012', 'Which company created the Java programming language?', 'Sun Microsystems', 'IBM', 'Oracle', 'Microsoft', 0, 'Tech', 'MEDIUM', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060013', 'TRIVIA_GK_013', 'What is the default number of players for a doubles table tennis team?', '2', '3', '4', '5', 2, 'General Knowledge', 'MEDIUM', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060014', 'TRIVIA_GK_014', 'Which metric is usually shown as a percentage in match summaries?', 'Wins', 'XP', 'Win Rate', 'Level', 2, 'Office Culture', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060015', 'TRIVIA_GK_015', 'Which SQL command is used to retrieve data from a table?', 'INSERT', 'DELETE', 'SELECT', 'UPDATE', 2, 'Tech', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060016', 'TRIVIA_GK_016', 'What is 9 x 7?', '56', '63', '72', '81', 1, 'General Knowledge', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060017', 'TRIVIA_GK_017', 'Which protocol is commonly used for secure website traffic?', 'FTP', 'SMTP', 'HTTPS', 'TELNET', 2, 'Tech', 'MEDIUM', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060018', 'TRIVIA_GK_018', 'In a standard work week, how many weekdays are there?', '4', '5', '6', '7', 1, 'Office Culture', 'EASY', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060019', 'TRIVIA_GK_019', 'Which ocean is the largest on Earth?', 'Atlantic Ocean', 'Indian Ocean', 'Arctic Ocean', 'Pacific Ocean', 3, 'General Knowledge', 'MEDIUM', TRUE, NOW(), NOW()),
    ('00000000-0000-0000-0000-000000060020', 'TRIVIA_GK_020', 'Which command-line tool is used for version control in this repository?', 'npm', 'git', 'docker', 'mvn', 1, 'Tech', 'EASY', TRUE, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;
