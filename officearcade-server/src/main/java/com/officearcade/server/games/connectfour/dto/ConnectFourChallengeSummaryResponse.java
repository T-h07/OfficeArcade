package com.officearcade.server.games.connectfour.dto;

public record ConnectFourChallengeSummaryResponse(
        String challengeId,
        String challengeTypeCode,
        String challengeTypeDisplayName,
        String status,
        String obligatedUserId,
        String beneficiaryUserId,
        int respectPointsAwarded,
        int karmaPointsAwarded,
        String createdAt,
        String resolvedAt
) {
}
