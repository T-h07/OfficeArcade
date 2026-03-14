package com.officearcade.server.admin.users.dto;

import com.officearcade.server.identity.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAdminUserRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Email format is invalid.")
        String email,
        @NotBlank(message = "Display name is required.")
        @Size(min = 2, max = 80, message = "Display name must be between 2 and 80 characters.")
        String displayName,
        @NotNull(message = "Role is required.")
        AppRole role
) {
}
