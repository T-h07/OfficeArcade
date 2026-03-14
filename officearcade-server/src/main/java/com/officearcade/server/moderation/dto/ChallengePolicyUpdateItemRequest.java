package com.officearcade.server.moderation.dto;

import jakarta.validation.constraints.NotBlank;

public record ChallengePolicyUpdateItemRequest(
        @NotBlank String code,
        boolean enabled
) {
}
