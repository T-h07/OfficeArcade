package com.officearcade.server.moderation.dto;

import jakarta.validation.constraints.Size;

public record AdminSuspendUserRequest(
        @Size(max = 240) String note
) {
}
