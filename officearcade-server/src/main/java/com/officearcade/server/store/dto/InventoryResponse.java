package com.officearcade.server.store.dto;

import java.util.List;

public record InventoryResponse(
        int respectBalance,
        int ownedCount,
        int equippedCount,
        List<InventoryItemResponse> items
) {
}
