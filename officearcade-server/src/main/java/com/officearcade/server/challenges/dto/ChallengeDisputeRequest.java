package com.officearcade.server.challenges.dto;

import jakarta.validation.constraints.Size;

public record ChallengeDisputeRequest(
        @Size(max = 280) String note
) {
}
