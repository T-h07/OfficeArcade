package com.officearcade.server.profile.dto;

public record ProfileEquippedCosmeticResponse(
        String cosmeticItemId,
        String code,
        String displayName,
        String category,
        String rarity,
        String previewAssetKey,
        int layerOrder,
        String equippedAt
) {
}
