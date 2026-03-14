package com.officearcade.server.users;

import com.officearcade.server.identity.AppRole;

public record UserQuery(
        String search,
        AppRole role,
        Boolean enabled,
        String departmentFilter
) {
}
