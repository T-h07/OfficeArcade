package com.officearcade.server.admin.users.dto;

import java.util.List;

public record AdminUserListResponse(
        List<AdminUserResponse> users,
        int total
) {
}
