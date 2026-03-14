package com.officearcade.server.leaderboards;

public enum LeaderboardType {
    WINS(
            "Wins",
            "Wins",
            "DESC",
            "Ranked by total wins. Ties share rank; stable ordering uses losses and user identity."
    ),
    WIN_RATE(
            "Win Rate",
            "Win Rate %",
            "DESC",
            "Ranked by win rate percentage for users with at least 5 completed matches.",
            5
    ),
    LEVEL(
            "Level",
            "Level",
            "DESC",
            "Ranked by level, then XP for tie separation."
    ),
    RESPECT(
            "Respect",
            "Respect Points",
            "DESC",
            "Ranked by total Respect points."
    ),
    KARMA(
            "Karma",
            "Karma Points",
            "ASC",
            "Lower Karma ranks higher in this view."
    ),
    GAMES_PLAYED(
            "Games Played",
            "Games Played",
            "DESC",
            "Ranked by total games played."
    );

    private final String title;
    private final String metricLabel;
    private final String rankingDirection;
    private final String description;
    private final int minimumCompletedMatches;

    LeaderboardType(String title, String metricLabel, String rankingDirection, String description) {
        this(title, metricLabel, rankingDirection, description, 0);
    }

    LeaderboardType(
            String title,
            String metricLabel,
            String rankingDirection,
            String description,
            int minimumCompletedMatches
    ) {
        this.title = title;
        this.metricLabel = metricLabel;
        this.rankingDirection = rankingDirection;
        this.description = description;
        this.minimumCompletedMatches = minimumCompletedMatches;
    }

    public String title() {
        return title;
    }

    public String metricLabel() {
        return metricLabel;
    }

    public String rankingDirection() {
        return rankingDirection;
    }

    public String description() {
        return description;
    }

    public int minimumCompletedMatches() {
        return minimumCompletedMatches;
    }
}
