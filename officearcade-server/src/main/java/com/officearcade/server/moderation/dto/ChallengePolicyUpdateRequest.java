package com.officearcade.server.moderation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ChallengePolicyUpdateRequest(
        @NotEmpty List<@Valid ChallengePolicyUpdateItemRequest> policies
) {
}
