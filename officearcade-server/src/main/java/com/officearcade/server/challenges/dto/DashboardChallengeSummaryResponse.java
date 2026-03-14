package com.officearcade.server.challenges.dto;

public record DashboardChallengeSummaryResponse(
        String id,
        String challengeTypeCode,
        String challengeTypeDisplayName,
        String status,
        String myRole,
        String counterpartyDisplayName,
        String createdAt,
        String resolvedAt
) {
}
