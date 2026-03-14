package com.officearcade.server.moderation.dto;

import java.util.List;

public record ModerationReportListResponse(
        int total,
        List<ModerationReportResponse> reports
) {
}
