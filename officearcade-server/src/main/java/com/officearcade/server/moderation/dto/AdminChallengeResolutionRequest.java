package com.officearcade.server.moderation.dto;

import com.officearcade.server.moderation.ChallengeDisputeResolutionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminChallengeResolutionRequest(
        @NotNull ChallengeDisputeResolutionType resolutionType,
        @Size(max = 280) String note
) {
}
