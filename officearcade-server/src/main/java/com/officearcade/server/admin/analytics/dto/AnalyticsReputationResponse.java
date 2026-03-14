package com.officearcade.server.admin.analytics.dto;

public record AnalyticsReputationResponse(
        int challengesCreatedInRange,
        int challengesPending,
        int challengesDisputed,
        int confirmedChallengesInRange,
        int rejectedChallengesInRange,
        int respectAwardedInRange,
        int karmaAppliedInRange
) {
}
