package com.officearcade.server.auth.dto;

public record RoleCheckResponse(
        String status,
        String route,
        String role
) {
}
