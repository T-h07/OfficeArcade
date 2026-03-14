package com.officearcade.server.admin.analytics.dto;

public record AnalyticsCategoryCountResponse(
        String category,
        int count
) {
}
