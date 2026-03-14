package com.officearcade.server.moderation.dto;

import com.officearcade.server.moderation.ModerationReportCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateModerationReportRequest(
        @NotBlank String reportedUserId,
        @NotNull ModerationReportCategory category,
        @Size(max = 280) String note,
        String sourceRoomId,
        String sourceGameSessionId,
        String sourceChallengeId
) {
}
