package com.officearcade.server.employee.dashboard.dto;

import com.officearcade.server.challenges.dto.DashboardChallengeSummaryResponse;
import com.officearcade.server.departments.dto.DepartmentSummaryResponse;
import java.util.List;

public record EmployeeDashboardResponse(
        String userId,
        String displayName,
        String email,
        String role,
        boolean accountEnabled,
        DepartmentSummaryResponse department,
        int level,
        int xp,
        int respectPoints,
        int karmaPoints,
        int gamesPlayed,
        int wins,
        int losses,
        int totalMatches,
        double winRatePercent,
        int xpForNextLevel,
        int xpToNextLevel,
        double xpProgressPercent,
        int enabledGameTypeCount,
        List<DashboardGameTypeResponse> enabledGameTypes,
        int ownedCosmeticCount,
        int equippedCosmeticCount,
        List<DashboardEquippedCosmeticResponse> equippedCosmetics,
        int pendingChallengeCount,
        int resolvedChallengeCount,
        List<DashboardChallengeSummaryResponse> recentChallenges,
        String profileUpdatedAt,
        String generatedAt
) {
}
