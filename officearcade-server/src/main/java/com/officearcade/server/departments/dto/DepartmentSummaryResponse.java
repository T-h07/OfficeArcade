package com.officearcade.server.departments.dto;

public record DepartmentSummaryResponse(
        String id,
        String code,
        String displayName,
        boolean active
) {
}
