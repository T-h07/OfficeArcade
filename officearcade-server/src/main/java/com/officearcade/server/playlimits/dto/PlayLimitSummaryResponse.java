package com.officearcade.server.playlimits.dto;

public record PlayLimitSummaryResponse(
        String userId,
        int dailyGameLimit,
        int gamesPlayedToday,
        int gamesRemainingToday,
        boolean cooldownActive,
        String cooldownUntil,
        long cooldownRemainingSeconds,
        boolean canPlayNow,
        String eligibilityReason,
        String gamesPlayedDate,
        String nextDailyResetAt,
        String lastCompletedGameAt,
        String updatedAt
) {
}
