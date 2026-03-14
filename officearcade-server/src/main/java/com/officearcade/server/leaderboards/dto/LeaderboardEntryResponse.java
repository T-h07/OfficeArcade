package com.officearcade.server.leaderboards.dto;

public record LeaderboardEntryResponse(
        int rank,
        String userId,
        String displayName,
        String role,
        int level,
        int xp,
        int gamesPlayed,
        int wins,
        int losses,
        int respectPoints,
        int karmaPoints,
        double winRatePercent,
        double primaryMetricValue,
        String primaryMetricDisplay,
        String profileFrameAssetKey,
        String badgeAssetKey,
        boolean currentUser
) {
}
