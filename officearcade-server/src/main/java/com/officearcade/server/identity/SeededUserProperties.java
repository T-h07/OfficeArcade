package com.officearcade.server.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "officearcade.auth")
public record SeededUserProperties(
        @NotEmpty List<SeededUser> seededUsers
) {
    public record SeededUser(
            @NotBlank String id,
            @NotBlank String email,
            @NotBlank String displayName,
            @NotBlank String passwordHash,
            @NotNull AppRole role,
            boolean enabled
    ) {
    }
}
