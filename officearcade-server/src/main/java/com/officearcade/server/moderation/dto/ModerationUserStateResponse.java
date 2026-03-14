package com.officearcade.server.moderation.dto;

public record ModerationUserStateResponse(
        String userId,
        String displayName,
        boolean suspended,
        String suspendedAt,
        String suspensionNote
) {
}
