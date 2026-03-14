package com.officearcade.server.admin.analytics.dto;

public record AnalyticsActivityPointResponse(
        String date,
        int activeUsers,
        int matchesPlayed,
        int roomsCreated
) {
}
