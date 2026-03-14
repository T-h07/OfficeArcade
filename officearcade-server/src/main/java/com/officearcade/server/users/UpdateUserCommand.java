package com.officearcade.server.users;

import com.officearcade.server.identity.AppRole;

public record UpdateUserCommand(
        String id,
        String email,
        String displayName,
        AppRole role
) {
}
