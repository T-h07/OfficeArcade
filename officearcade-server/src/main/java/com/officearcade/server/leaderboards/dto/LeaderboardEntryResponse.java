package com.officearcade.server.leaderboards.dto;

import com.officearcade.server.departments.dto.DepartmentSummaryResponse;

public record LeaderboardEntryResponse(
        int rank,
        String userId,
        String displayName,
        String role,
        DepartmentSummaryResponse department,
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
