package com.officearcade.server.challenges.dto;

public record ChallengeSummaryResponse(
        String id,
        String sourceGameSessionId,
        String sourceRoomId,
        String challengeTypeCode,
        String challengeTypeDisplayName,
        String challengeTypeDescription,
        String status,
        String obligatedUserId,
        String obligatedDisplayName,
        String beneficiaryUserId,
        String beneficiaryDisplayName,
        String myRole,
        boolean canResolve,
        int respectPointsAwarded,
        int karmaPointsAwarded,
        String createdAt,
        String resolvedAt,
        String disputedAt,
        String disputeNote,
        String resolutionNote,
        String resolvedByAdminId
) {
}
