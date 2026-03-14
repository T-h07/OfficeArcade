package com.officearcade.server.moderation.dto;

public record ModerationAuditEntryResponse(
        String id,
        String adminUserId,
        String adminDisplayName,
        String targetUserId,
        String targetDisplayName,
        String actionType,
        String reportId,
        String challengeId,
        String note,
        String createdAt
) {
}
