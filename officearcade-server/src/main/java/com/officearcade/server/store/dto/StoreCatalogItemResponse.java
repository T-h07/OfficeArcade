package com.officearcade.server.store.dto;

public record StoreCatalogItemResponse(
        String id,
        String code,
        String displayName,
        String description,
        String category,
        String rarity,
        int priceRespect,
        String previewAssetKey,
        boolean enabled,
        boolean owned,
        boolean equipped
) {
}
