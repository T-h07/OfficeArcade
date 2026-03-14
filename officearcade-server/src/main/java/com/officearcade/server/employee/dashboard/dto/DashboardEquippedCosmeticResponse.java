package com.officearcade.server.employee.dashboard.dto;

public record DashboardEquippedCosmeticResponse(
        String cosmeticItemId,
        String code,
        String displayName,
        String category,
        String rarity,
        String previewAssetKey,
        String equippedAt
) {
}
