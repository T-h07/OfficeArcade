package com.officearcade.server.employee.dashboard.dto;

import java.util.List;

public record EmployeeDashboardResponse(
        String userId,
        String displayName,
        String email,
        String role,
        boolean accountEnabled,
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
        String profileUpdatedAt,
        String generatedAt
) {
}
