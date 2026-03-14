package com.officearcade.server.moderation.dto;

public record AdminChallengeReviewResponse(
        String challengeId,
        String sourceRoomId,
        String sourceGameSessionId,
        String challengeTypeCode,
        String challengeTypeDisplayName,
        String status,
        String obligatedUserId,
        String obligatedDisplayName,
        String beneficiaryUserId,
        String beneficiaryDisplayName,
        String disputeNote,
        String disputedAt,
        String createdAt
) {
}
