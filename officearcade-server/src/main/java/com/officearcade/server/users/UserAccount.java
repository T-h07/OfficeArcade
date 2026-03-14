package com.officearcade.server.users;

import com.officearcade.server.identity.AppRole;
import java.time.Instant;

public record UserAccount(
        String id,
        String email,
        String displayName,
        String passwordHash,
        AppRole role,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}
