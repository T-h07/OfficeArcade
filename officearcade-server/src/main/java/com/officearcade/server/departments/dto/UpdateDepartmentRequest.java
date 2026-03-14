package com.officearcade.server.departments.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateDepartmentRequest(
        @NotBlank(message = "Department code is required.")
        @Size(min = 2, max = 40, message = "Department code must be between 2 and 40 characters.")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "Department code may only contain letters, numbers, hyphen, and underscore."
        )
        String code,
        @NotBlank(message = "Department display name is required.")
        @Size(min = 2, max = 120, message = "Department display name must be between 2 and 120 characters.")
        String displayName,
        @Size(max = 280, message = "Department description must be 280 characters or fewer.")
        String description
) {
}
