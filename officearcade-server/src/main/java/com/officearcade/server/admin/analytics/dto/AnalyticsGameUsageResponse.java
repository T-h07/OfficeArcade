package com.officearcade.server.admin.analytics.dto;

public record AnalyticsGameUsageResponse(
        String gameTypeCode,
        String gameTypeDisplayName,
        int matchesPlayed,
        double percentOfMatches
) {
}
