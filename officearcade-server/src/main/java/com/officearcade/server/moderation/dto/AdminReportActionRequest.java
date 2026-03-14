package com.officearcade.server.moderation.dto;

import com.officearcade.server.moderation.ModerationReportActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminReportActionRequest(
        @NotNull ModerationReportActionType actionType,
        @Size(max = 280) String note
) {
}
