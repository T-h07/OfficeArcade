package com.officearcade.server.admin.users.dto;

public record AdminUserResponse(
        String id,
        String email,
        String displayName,
        String role,
        boolean enabled,
        String createdAt,
        String updatedAt
) {
}
