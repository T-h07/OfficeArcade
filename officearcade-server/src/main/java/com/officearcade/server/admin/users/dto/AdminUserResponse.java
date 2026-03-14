package com.officearcade.server.admin.users.dto;

import com.officearcade.server.departments.dto.DepartmentSummaryResponse;

public record AdminUserResponse(
        String id,
        String email,
        String displayName,
        String role,
        boolean enabled,
        DepartmentSummaryResponse department,
        String createdAt,
        String updatedAt
) {
}
