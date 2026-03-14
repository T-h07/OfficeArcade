package com.officearcade.server.store.dto;

public record EquippedCosmeticSummaryResponse(
        String cosmeticItemId,
        String code,
        String displayName,
        String category,
        String rarity,
        String previewAssetKey,
        String equippedAt
) {
}
