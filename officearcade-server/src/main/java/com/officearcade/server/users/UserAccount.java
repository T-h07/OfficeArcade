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
        boolean suspended,
        Instant suspendedAt,
        String suspensionNote,
        String suspendedByAdminId,
        Instant createdAt,
        Instant updatedAt
) {
}
