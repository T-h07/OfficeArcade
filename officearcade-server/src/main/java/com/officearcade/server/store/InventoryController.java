package com.officearcade.server.store;

import com.officearcade.server.security.OfficeArcadePrincipal;
import com.officearcade.server.store.dto.InventoryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class InventoryController {

    private final StoreInventoryService storeInventoryService;

    public InventoryController(StoreInventoryService storeInventoryService) {
        this.storeInventoryService = storeInventoryService;
    }

    @GetMapping("/me")
    public InventoryResponse getMyInventory(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        return storeInventoryService.getInventory(requiredPrincipal(principal).id());
    }

    @PostMapping("/equip/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse equipItem(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String itemId
    ) {
        return storeInventoryService.equipItem(requiredPrincipal(principal).id(), itemId);
    }

    @PostMapping("/unequip/{itemId}")
    @ResponseStatus(HttpStatus.OK)
    public InventoryResponse unequipItem(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String itemId
    ) {
        return storeInventoryService.unequipItem(requiredPrincipal(principal).id(), itemId);
    }

    private static OfficeArcadePrincipal requiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
