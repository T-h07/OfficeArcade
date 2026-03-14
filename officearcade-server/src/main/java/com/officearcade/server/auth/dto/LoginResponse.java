package com.officearcade.server.auth.dto;

public record LoginResponse(
        String tokenType,
        String accessToken,
        AuthenticatedUserResponse user
) {
}
