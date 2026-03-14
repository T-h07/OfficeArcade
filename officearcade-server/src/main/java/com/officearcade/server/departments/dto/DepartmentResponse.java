package com.officearcade.server.departments.dto;

public record DepartmentResponse(
        String id,
        String code,
        String displayName,
        String description,
        boolean active,
        int assignedUserCount,
        String createdAt,
        String updatedAt
) {
}
