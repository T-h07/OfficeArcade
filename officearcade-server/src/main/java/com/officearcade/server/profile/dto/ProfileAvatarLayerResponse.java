package com.officearcade.server.profile.dto;

public record ProfileAvatarLayerResponse(
        String layerKey,
        String category,
        int layerOrder,
        String displayName,
        String previewAssetKey,
        String source,
        String cosmeticItemId
) {
}
