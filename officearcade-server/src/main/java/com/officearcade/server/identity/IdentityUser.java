package com.officearcade.server.identity;

public record IdentityUser(
        String id,
        String email,
        String displayName,
        String passwordHash,
        AppRole role,
        boolean enabled
) {
}
