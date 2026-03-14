package com.officearcade.server.moderation.dto;

import jakarta.validation.constraints.Size;

public record AdminDismissReportRequest(
        @Size(max = 280) String note
) {
}
