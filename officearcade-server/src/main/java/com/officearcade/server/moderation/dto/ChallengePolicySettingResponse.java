package com.officearcade.server.moderation.dto;

public record ChallengePolicySettingResponse(
        String code,
        String displayName,
        String description,
        int respectRewardPoints,
        int karmaPenaltyPoints,
        boolean enabled,
        String updatedAt
) {
}
