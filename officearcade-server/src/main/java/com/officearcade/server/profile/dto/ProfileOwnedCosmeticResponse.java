package com.officearcade.server.profile.dto;

public record ProfileOwnedCosmeticResponse(
        String cosmeticItemId,
        String code,
        String displayName,
        String description,
        String category,
        String rarity,
        String previewAssetKey,
        boolean enabled,
        boolean equipped,
        String acquiredAt,
        String equippedAt
) {
}
