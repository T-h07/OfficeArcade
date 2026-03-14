package com.officearcade.server.moderation.dto;

import java.util.List;

public record ChallengePolicyListResponse(
        int total,
        List<ChallengePolicySettingResponse> policies
) {
}
