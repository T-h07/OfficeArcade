package com.officearcade.server.admin.analytics.dto;

import java.util.List;

public record AnalyticsModerationResponse(
        int openReports,
        int inReviewReports,
        int resolvedReports,
        int dismissedReports,
        int reportsCreatedInRange,
        int suspendedUsers,
        List<AnalyticsCategoryCountResponse> reportCategoriesInRange
) {
}
