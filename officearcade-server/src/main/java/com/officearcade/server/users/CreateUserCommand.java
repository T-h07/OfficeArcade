package com.officearcade.server.users;

import com.officearcade.server.identity.AppRole;

public record CreateUserCommand(
        String email,
        String displayName,
        String rawPassword,
        AppRole role,
        boolean enabled
) {
}
