package com.officearcade.server.admin.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordRequest(
        @NotBlank(message = "New password is required.")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters.")
        @Pattern(
                regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "Password must include at least one letter and one number."
        )
        String newPassword
) {
}
