package com.officearcade.server.leaderboards.dto;

import java.util.List;

public record LeaderboardTypeListResponse(
        List<LeaderboardTypeOptionResponse> types,
        String generatedAt
) {
}
