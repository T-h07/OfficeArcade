package com.officearcade.server.store.dto;

import java.util.List;

public record StoreSummaryResponse(
        int respectBalance,
        int ownedCount,
        int equippedCount,
        List<EquippedCosmeticSummaryResponse> equippedItems
) {
}
