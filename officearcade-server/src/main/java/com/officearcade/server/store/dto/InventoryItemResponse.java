package com.officearcade.server.store.dto;

public record InventoryItemResponse(
        String cosmeticItemId,
        String code,
        String displayName,
        String description,
        String category,
        String rarity,
        int priceRespect,
        String previewAssetKey,
        boolean enabled,
        boolean equipped,
        String acquiredAt,
        String equippedAt
) {
}
