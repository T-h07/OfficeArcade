package com.officearcade.server.auth.dto;

import com.officearcade.server.departments.dto.DepartmentSummaryResponse;

public record AuthenticatedUserResponse(
        String id,
        String email,
        String displayName,
        String role,
        boolean enabled,
        boolean suspended,
        String suspendedAt,
        String suspensionNote,
        DepartmentSummaryResponse department
) {
}
