package com.officearcade.server.store.dto;

public record StorePurchaseResponse(
        String status,
        String message,
        StoreSummaryResponse summary,
        StoreCatalogItemResponse item
) {
}
