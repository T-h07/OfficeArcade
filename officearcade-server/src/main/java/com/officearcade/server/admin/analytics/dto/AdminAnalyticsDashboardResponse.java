package com.officearcade.server.admin.analytics.dto;

import java.util.List;

public record AdminAnalyticsDashboardResponse(
        AnalyticsFilterResponse filters,
        AnalyticsSummaryResponse summary,
        List<AnalyticsActivityPointResponse> activityTrend,
        String activityTrendLabel,
        List<AnalyticsGameUsageResponse> gameUsage,
        List<AnalyticsDepartmentInsightResponse> departments,
        AnalyticsReputationResponse reputation,
        AnalyticsModerationResponse moderation,
        String generatedAt
) {
}
