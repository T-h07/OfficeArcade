package com.officearcade.server.admin.users.dto;

import jakarta.validation.constraints.Size;

public record AssignUserDepartmentRequest(
        @Size(max = 64, message = "Department id must be a UUID value.")
        String departmentId
) {
}
