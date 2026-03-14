package com.officearcade.server.auth.dto;

public record AuthenticatedUserResponse(
        String id,
        String email,
        String displayName,
        String role,
        boolean enabled
) {
}
