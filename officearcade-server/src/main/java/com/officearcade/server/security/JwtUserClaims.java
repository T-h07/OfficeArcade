package com.officearcade.server.security;

import com.officearcade.server.identity.AppRole;

public record JwtUserClaims(
        String userId,
        String email,
        String displayName,
        AppRole role
) {
}
