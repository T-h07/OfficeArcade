package com.officearcade.server.challenges.dto;

import java.util.List;

public record ChallengeListResponse(
        int totalCount,
        int pendingCount,
        int resolvedCount,
        List<ChallengeSummaryResponse> challenges
) {
}
