package com.officearcade.server.store;

import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.store.dto.EquippedCosmeticSummaryResponse;
import com.officearcade.server.store.dto.InventoryItemResponse;
import com.officearcade.server.store.dto.InventoryResponse;
import com.officearcade.server.store.dto.StoreCatalogItemResponse;
import com.officearcade.server.store.dto.StoreCatalogResponse;
import com.officearcade.server.store.dto.StorePurchaseResponse;
import com.officearcade.server.store.dto.StoreSummaryResponse;
import com.officearcade.server.store.persistence.CosmeticItemEntity;
import com.officearcade.server.store.persistence.CosmeticItemEntityRepository;
import com.officearcade.server.store.persistence.UserEquippedCosmeticEntity;
import com.officearcade.server.store.persistence.UserEquippedCosmeticEntityRepository;
import com.officearcade.server.store.persistence.UserOwnedCosmeticEntity;
import com.officearcade.server.store.persistence.UserOwnedCosmeticEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoreInventoryService {

    private final CosmeticItemEntityRepository cosmeticItemEntityRepository;
    private final UserOwnedCosmeticEntityRepository userOwnedCosmeticEntityRepository;
    private final UserEquippedCosmeticEntityRepository userEquippedCosmeticEntityRepository;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final UserEntityRepository userEntityRepository;

    public StoreInventoryService(
            CosmeticItemEntityRepository cosmeticItemEntityRepository,
            UserOwnedCosmeticEntityRepository userOwnedCosmeticEntityRepository,
            UserEquippedCosmeticEntityRepository userEquippedCosmeticEntityRepository,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            UserEntityRepository userEntityRepository
    ) {
        this.cosmeticItemEntityRepository = cosmeticItemEntityRepository;
        this.userOwnedCosmeticEntityRepository = userOwnedCosmeticEntityRepository;
        this.userEquippedCosmeticEntityRepository = userEquippedCosmeticEntityRepository;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.userEntityRepository = userEntityRepository;
    }

    @Transactional(readOnly = true)
    public StoreCatalogResponse listCatalog(String currentUserIdText, CosmeticCategory category, CosmeticRarity rarity) {
        UUID userId = parseUserId(currentUserIdText);
        PlayerProfileEntity profile = getRequiredProfile(userId);

        Specification<CosmeticItemEntity> specification = Specification.where((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("enabled"), true));

        if (category != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("category"), category));
        }
        if (rarity != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("rarity"), rarity));
        }

        List<CosmeticItemEntity> items = cosmeticItemEntityRepository.findAll(
                specification,
                Sort.by(Sort.Order.asc("priceRespect"), Sort.Order.asc("displayName"))
        );

        Set<UUID> ownedItemIds = userOwnedCosmeticEntityRepository.findAllByUser_IdOrderByAcquiredAtDesc(userId).stream()
                .map(owned -> owned.getCosmeticItem().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        Set<UUID> equippedItemIds = userEquippedCosmeticEntityRepository.findAllByUser_IdOrderByEquippedAtDesc(userId).stream()
                .map(equipped -> equipped.getCosmeticItem().getId())
                .collect(HashSet::new, HashSet::add, HashSet::addAll);

        List<StoreCatalogItemResponse> mapped = items.stream()
                .map(item -> toCatalogResponse(item, ownedItemIds.contains(item.getId()), equippedItemIds.contains(item.getId())))
                .toList();

        return new StoreCatalogResponse(Math.max(profile.getRespectPoints(), 0), mapped.size(), mapped);
    }

    @Transactional(readOnly = true)
    public StoreCatalogItemResponse getCatalogItem(String currentUserIdText, String itemIdText) {
        UUID userId = parseUserId(currentUserIdText);
        UUID itemId = parseItemId(itemIdText);

        CosmeticItemEntity item = getRequiredCosmeticItem(itemId);
        boolean owned = userOwnedCosmeticEntityRepository.existsByUser_IdAndCosmeticItem_Id(userId, itemId);
        boolean equipped = userEquippedCosmeticEntityRepository.findByUser_IdAndCosmeticItem_Id(userId, itemId).isPresent();
        return toCatalogResponse(item, owned, equipped);
    }

    @Transactional(readOnly = true)
    public StoreSummaryResponse getStoreSummary(String currentUserIdText) {
        UUID userId = parseUserId(currentUserIdText);
        PlayerProfileEntity profile = getRequiredProfile(userId);
        return buildStoreSummary(userId, profile);
    }

    @Transactional
    public StorePurchaseResponse purchaseItem(String currentUserIdText, String itemIdText) {
        UUID userId = parseUserId(currentUserIdText);
        UUID itemId = parseItemId(itemIdText);

        UserEntity user = getRequiredUser(userId);
        CosmeticItemEntity item = getRequiredCosmeticItem(itemId);
        if (!item.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This cosmetic item is currently disabled.");
        }

        if (userOwnedCosmeticEntityRepository.existsByUser_IdAndCosmeticItem_Id(userId, itemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already own this cosmetic item.");
        }

        PlayerProfileEntity profile = playerProfileEntityRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player profile is not initialized for this user."
                ));

        if (profile.getRespectPoints() < item.getPriceRespect()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Insufficient Respect points for this purchase."
            );
        }

        profile.setRespectPoints(profile.getRespectPoints() - item.getPriceRespect());
        playerProfileEntityRepository.save(profile);

        try {
            UserOwnedCosmeticEntity owned = new UserOwnedCosmeticEntity();
            owned.setUser(user);
            owned.setCosmeticItem(item);
            owned.setAcquiredAt(Instant.now());
            userOwnedCosmeticEntityRepository.save(owned);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Purchase could not be completed because this item is already owned."
            );
        }

        StoreSummaryResponse summary = buildStoreSummary(userId, profile);
        StoreCatalogItemResponse purchased = toCatalogResponse(item, true, false);
        return new StorePurchaseResponse("OK", "Purchase completed.", summary, purchased);
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(String currentUserIdText) {
        UUID userId = parseUserId(currentUserIdText);
        PlayerProfileEntity profile = getRequiredProfile(userId);

        List<UserOwnedCosmeticEntity> owned = userOwnedCosmeticEntityRepository.findAllByUser_IdOrderByAcquiredAtDesc(userId);
        List<UserEquippedCosmeticEntity> equipped = userEquippedCosmeticEntityRepository.findAllByUser_IdOrderByEquippedAtDesc(userId);

        Map<UUID, UserEquippedCosmeticEntity> equippedByItemId = new HashMap<>();
        for (UserEquippedCosmeticEntity entry : equipped) {
            equippedByItemId.put(entry.getCosmeticItem().getId(), entry);
        }

        List<InventoryItemResponse> items = new ArrayList<>(owned.size());
        for (UserOwnedCosmeticEntity ownedEntry : owned) {
            CosmeticItemEntity item = ownedEntry.getCosmeticItem();
            UserEquippedCosmeticEntity equippedEntry = equippedByItemId.get(item.getId());
            items.add(new InventoryItemResponse(
                    item.getId().toString(),
                    item.getCode(),
                    item.getDisplayName(),
                    item.getDescription(),
                    item.getCategory().name(),
                    item.getRarity().name(),
                    item.getPriceRespect(),
                    item.getPreviewAssetKey(),
                    item.isEnabled(),
                    equippedEntry != null,
                    ownedEntry.getAcquiredAt().toString(),
                    equippedEntry == null ? null : equippedEntry.getEquippedAt().toString()
            ));
        }

        return new InventoryResponse(
                Math.max(profile.getRespectPoints(), 0),
                items.size(),
                equipped.size(),
                items
        );
    }

    @Transactional
    public InventoryResponse equipItem(String currentUserIdText, String itemIdText) {
        UUID userId = parseUserId(currentUserIdText);
        UUID itemId = parseItemId(itemIdText);

        UserEntity user = getRequiredUser(userId);
        CosmeticItemEntity item = getRequiredCosmeticItem(itemId);
        if (!item.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This cosmetic item is currently disabled.");
        }

        if (!userOwnedCosmeticEntityRepository.existsByUser_IdAndCosmeticItem_Id(userId, itemId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You must own an item before equipping it.");
        }

        Optional<UserEquippedCosmeticEntity> equippedByItem = userEquippedCosmeticEntityRepository
                .findByUser_IdAndCosmeticItem_Id(userId, itemId);
        if (equippedByItem.isPresent()) {
            return getInventory(currentUserIdText);
        }

        Optional<UserEquippedCosmeticEntity> existingInCategory = userEquippedCosmeticEntityRepository
                .findByUser_IdAndCategory(userId, item.getCategory());
        if (existingInCategory.isPresent()) {
            userEquippedCosmeticEntityRepository.delete(existingInCategory.get());
            userEquippedCosmeticEntityRepository.flush();
        }

        UserEquippedCosmeticEntity equip = new UserEquippedCosmeticEntity();
        equip.setUser(user);
        equip.setCosmeticItem(item);
        equip.setCategory(item.getCategory());
        equip.setEquippedAt(Instant.now());
        userEquippedCosmeticEntityRepository.save(equip);

        return getInventory(currentUserIdText);
    }

    @Transactional
    public InventoryResponse unequipItem(String currentUserIdText, String itemIdText) {
        UUID userId = parseUserId(currentUserIdText);
        UUID itemId = parseItemId(itemIdText);

        UserEquippedCosmeticEntity equipped = userEquippedCosmeticEntityRepository
                .findByUser_IdAndCosmeticItem_Id(userId, itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This item is not currently equipped."
                ));

        userEquippedCosmeticEntityRepository.delete(equipped);
        return getInventory(currentUserIdText);
    }

    private StoreSummaryResponse buildStoreSummary(UUID userId, PlayerProfileEntity profile) {
        List<UserEquippedCosmeticEntity> equippedEntries = userEquippedCosmeticEntityRepository
                .findAllByUser_IdOrderByEquippedAtDesc(userId);

        List<EquippedCosmeticSummaryResponse> equippedItems = equippedEntries.stream()
                .map(entry -> new EquippedCosmeticSummaryResponse(
                        entry.getCosmeticItem().getId().toString(),
                        entry.getCosmeticItem().getCode(),
                        entry.getCosmeticItem().getDisplayName(),
                        entry.getCategory().name(),
                        entry.getCosmeticItem().getRarity().name(),
                        entry.getCosmeticItem().getPreviewAssetKey(),
                        entry.getEquippedAt().toString()
                ))
                .toList();

        return new StoreSummaryResponse(
                Math.max(profile.getRespectPoints(), 0),
                (int) userOwnedCosmeticEntityRepository.countByUser_Id(userId),
                equippedItems.size(),
                equippedItems
        );
    }

    private static StoreCatalogItemResponse toCatalogResponse(CosmeticItemEntity item, boolean owned, boolean equipped) {
        return new StoreCatalogItemResponse(
                item.getId().toString(),
                item.getCode(),
                item.getDisplayName(),
                item.getDescription(),
                item.getCategory().name(),
                item.getRarity().name(),
                item.getPriceRespect(),
                item.getPreviewAssetKey(),
                item.isEnabled(),
                owned,
                equipped
        );
    }

    private CosmeticItemEntity getRequiredCosmeticItem(UUID itemId) {
        return cosmeticItemEntityRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cosmetic item not found: " + itemId
                ));
    }

    private PlayerProfileEntity getRequiredProfile(UUID userId) {
        return playerProfileEntityRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player profile is not initialized for this user."
                ));
    }

    private UserEntity getRequiredUser(UUID userId) {
        return userEntityRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user is no longer available."
                ));
    }

    private static UUID parseUserId(String userIdText) {
        try {
            return UUID.fromString(userIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        }
    }

    private static UUID parseItemId(String itemIdText) {
        try {
            return UUID.fromString(itemIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item id must be a valid UUID.");
        }
    }
}
