package com.officearcade.server.moderation.dto;

public record ModerationReportResponse(
        String id,
        String reporterUserId,
        String reporterDisplayName,
        String reportedUserId,
        String reportedDisplayName,
        String category,
        String note,
        String status,
        String sourceRoomId,
        String sourceGameSessionId,
        String sourceChallengeId,
        String reviewedByAdminId,
        String reviewedByAdminDisplayName,
        String resolutionNote,
        String createdAt,
        String updatedAt
) {
}
