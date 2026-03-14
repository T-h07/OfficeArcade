package com.officearcade.server.admin.users.dto;

import com.officearcade.server.identity.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAdminUserRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Email format is invalid.")
        String email,
        @NotBlank(message = "Display name is required.")
        @Size(min = 2, max = 80, message = "Display name must be between 2 and 80 characters.")
        String displayName,
        @NotBlank(message = "Password is required.")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must include at least one letter and one number."
        )
        String password,
        AppRole role,
        Boolean enabled
) {
}
