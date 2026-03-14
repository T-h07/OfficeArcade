package com.officearcade.server.auth.dto;

public record CurrentUserResponse(
        AuthenticatedUserResponse user
) {
}
