package com.officearcade.server.store;

import com.officearcade.server.security.OfficeArcadePrincipal;
import com.officearcade.server.store.dto.StoreCatalogItemResponse;
import com.officearcade.server.store.dto.StoreCatalogResponse;
import com.officearcade.server.store.dto.StorePurchaseResponse;
import com.officearcade.server.store.dto.StoreSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/store")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class StoreController {

    private final StoreInventoryService storeInventoryService;

    public StoreController(StoreInventoryService storeInventoryService) {
        this.storeInventoryService = storeInventoryService;
    }

    @GetMapping("/catalog")
    public StoreCatalogResponse listCatalog(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @RequestParam(required = false) CosmeticCategory category,
            @RequestParam(required = false) CosmeticRarity rarity
    ) {
        return storeInventoryService.listCatalog(requiredPrincipal(principal).id(), category, rarity);
    }

    @GetMapping("/catalog/{itemId}")
    public StoreCatalogItemResponse getCatalogItem(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String itemId
    ) {
        return storeInventoryService.getCatalogItem(requiredPrincipal(principal).id(), itemId);
    }

    @PostMapping("/purchase/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public StorePurchaseResponse purchaseItem(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String itemId
    ) {
        return storeInventoryService.purchaseItem(requiredPrincipal(principal).id(), itemId);
    }

    @GetMapping("/me/summary")
    public StoreSummaryResponse getStoreSummary(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        return storeInventoryService.getStoreSummary(requiredPrincipal(principal).id());
    }

    private static OfficeArcadePrincipal requiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
