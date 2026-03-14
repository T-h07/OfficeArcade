package com.officearcade.server.admin.analytics.dto;

public record AnalyticsSummaryResponse(
        int totalEnabledUsers,
        int activeUsersInRange,
        int activeUsersToday,
        int matchesInRange,
        int matchesToday,
        int roomsCreatedInRange,
        int totalDepartments,
        int respectAwardedInRange,
        int karmaAppliedInRange,
        int openModerationReports,
        int suspendedUsers,
        double averageMatchesPerActiveUser,
        String topDepartmentByParticipation
) {
}
