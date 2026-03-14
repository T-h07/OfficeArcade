package com.officearcade.server.moderation.dto;

import java.util.List;

public record ModerationAuditListResponse(
        int total,
        List<ModerationAuditEntryResponse> entries
) {
}
