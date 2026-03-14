package com.officearcade.server.leaderboards.dto;

import java.util.List;

public record LeaderboardResponse(
        String type,
        String title,
        String metricLabel,
        String rankingDirection,
        int minimumCompletedMatches,
        int limit,
        int totalEligibleEntries,
        List<LeaderboardEntryResponse> entries,
        LeaderboardEntryResponse currentUserEntry,
        boolean currentUserEligible,
        String currentUserNote,
        String generatedAt
) {
}
