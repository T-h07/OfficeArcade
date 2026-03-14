package com.officearcade.server.leaderboards.dto;

public record LeaderboardTypeOptionResponse(
        String type,
        String title,
        String metricLabel,
        String rankingDirection,
        int minimumCompletedMatches,
        String description
) {
}
