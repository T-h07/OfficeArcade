package com.officearcade.server.moderation.dto;

import java.util.List;

public record AdminChallengeReviewListResponse(
        int total,
        List<AdminChallengeReviewResponse> challenges
) {
}
