package com.officearcade.server.store.dto;

import java.util.List;

public record StoreCatalogResponse(
        int respectBalance,
        int totalItems,
        List<StoreCatalogItemResponse> items
) {
}
