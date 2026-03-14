package com.officearcade.server.profile;

import com.officearcade.server.departments.dto.DepartmentSummaryResponse;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.profile.dto.ProfileAvatarLayerResponse;
import com.officearcade.server.profile.dto.ProfileEquippedCosmeticResponse;
import com.officearcade.server.profile.dto.ProfileMeResponse;
import com.officearcade.server.profile.dto.ProfileOwnedCosmeticResponse;
import com.officearcade.server.store.CosmeticCategory;
import com.officearcade.server.store.persistence.UserEquippedCosmeticEntity;
import com.officearcade.server.store.persistence.UserEquippedCosmeticEntityRepository;
import com.officearcade.server.store.persistence.UserOwnedCosmeticEntity;
import com.officearcade.server.store.persistence.UserOwnedCosmeticEntityRepository;
import com.officearcade.server.users.UserAccount;
import com.officearcade.server.users.UserAccountService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileService {

    private static final String BASE_LAYER_CATEGORY = "BASE_BODY";
    private static final String BASE_LAYER_ASSET_KEY = "base.officearcade-default";

    private final UserAccountService userAccountService;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final UserOwnedCosmeticEntityRepository userOwnedCosmeticEntityRepository;
    private final UserEquippedCosmeticEntityRepository userEquippedCosmeticEntityRepository;

    public ProfileService(
            UserAccountService userAccountService,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            UserOwnedCosmeticEntityRepository userOwnedCosmeticEntityRepository,
            UserEquippedCosmeticEntityRepository userEquippedCosmeticEntityRepository
    ) {
        this.userAccountService = userAccountService;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.userOwnedCosmeticEntityRepository = userOwnedCosmeticEntityRepository;
        this.userEquippedCosmeticEntityRepository = userEquippedCosmeticEntityRepository;
    }

    @Transactional(readOnly = true)
    public ProfileMeResponse getProfileForUser(String userIdText) {
        UUID userId = parseUserId(userIdText);

        UserAccount user = userAccountService.findById(userId.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated."));

        PlayerProfileEntity profile = playerProfileEntityRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player profile is not initialized for user: " + user.id()
                ));

        List<UserOwnedCosmeticEntity> ownedEntries = userOwnedCosmeticEntityRepository
                .findAllByUser_IdOrderByAcquiredAtDesc(userId);
        Set<UUID> ownedIds = new HashSet<>();
        for (UserOwnedCosmeticEntity ownedEntry : ownedEntries) {
            ownedIds.add(ownedEntry.getCosmeticItem().getId());
        }

        List<ProfileEquippedCosmeticResponse> equippedCosmetics = userEquippedCosmeticEntityRepository
                .findAllByUser_IdOrderByEquippedAtDesc(userId)
                .stream()
                .filter(entry -> entry.getCosmeticItem().isEnabled())
                .filter(entry -> ownedIds.contains(entry.getCosmeticItem().getId()))
                .map(this::toEquippedResponse)
                .sorted(Comparator.comparingInt(ProfileEquippedCosmeticResponse::layerOrder))
                .toList();

        Map<UUID, ProfileEquippedCosmeticResponse> equippedByItemId = new HashMap<>();
        for (ProfileEquippedCosmeticResponse equippedCosmetic : equippedCosmetics) {
            equippedByItemId.put(UUID.fromString(equippedCosmetic.cosmeticItemId()), equippedCosmetic);
        }

        List<ProfileOwnedCosmeticResponse> ownedCosmetics = new ArrayList<>(ownedEntries.size());
        for (UserOwnedCosmeticEntity ownedEntry : ownedEntries) {
            ProfileEquippedCosmeticResponse equipped = equippedByItemId.get(ownedEntry.getCosmeticItem().getId());
            ownedCosmetics.add(new ProfileOwnedCosmeticResponse(
                    ownedEntry.getCosmeticItem().getId().toString(),
                    ownedEntry.getCosmeticItem().getCode(),
                    ownedEntry.getCosmeticItem().getDisplayName(),
                    ownedEntry.getCosmeticItem().getDescription(),
                    ownedEntry.getCosmeticItem().getCategory().name(),
                    ownedEntry.getCosmeticItem().getRarity().name(),
                    ownedEntry.getCosmeticItem().getPreviewAssetKey(),
                    ownedEntry.getCosmeticItem().isEnabled(),
                    equipped != null,
                    ownedEntry.getAcquiredAt().toString(),
                    equipped == null ? null : equipped.equippedAt()
            ));
        }

        List<ProfileAvatarLayerResponse> avatarLayers = new ArrayList<>();
        avatarLayers.add(new ProfileAvatarLayerResponse(
                "BASE_LAYER",
                BASE_LAYER_CATEGORY,
                10,
                "OfficeArcade Base",
                BASE_LAYER_ASSET_KEY,
                "BASE",
                null
        ));

        for (ProfileEquippedCosmeticResponse equippedCosmetic : equippedCosmetics) {
            avatarLayers.add(new ProfileAvatarLayerResponse(
                    "CATEGORY_" + equippedCosmetic.category(),
                    equippedCosmetic.category(),
                    equippedCosmetic.layerOrder(),
                    equippedCosmetic.displayName(),
                    equippedCosmetic.previewAssetKey(),
                    "COSMETIC",
                    equippedCosmetic.cosmeticItemId()
            ));
        }

        return new ProfileMeResponse(
                user.id(),
                user.displayName(),
                user.email(),
                user.role().name(),
                user.enabled(),
                toDepartmentSummary(user),
                Math.max(profile.getLevel(), 1),
                Math.max(profile.getXp(), 0),
                Math.max(profile.getRespectPoints(), 0),
                Math.max(profile.getKarmaPoints(), 0),
                ownedCosmetics.size(),
                equippedCosmetics.size(),
                ownedCosmetics,
                equippedCosmetics,
                avatarLayers,
                profile.getUpdatedAt().toString(),
                Instant.now().toString()
        );
    }

    private ProfileEquippedCosmeticResponse toEquippedResponse(UserEquippedCosmeticEntity entry) {
        return new ProfileEquippedCosmeticResponse(
                entry.getCosmeticItem().getId().toString(),
                entry.getCosmeticItem().getCode(),
                entry.getCosmeticItem().getDisplayName(),
                entry.getCategory().name(),
                entry.getCosmeticItem().getRarity().name(),
                entry.getCosmeticItem().getPreviewAssetKey(),
                layerOrderFor(entry.getCategory()),
                entry.getEquippedAt().toString()
        );
    }

    private static int layerOrderFor(CosmeticCategory category) {
        return switch (category) {
            case OUTFIT -> 20;
            case ACCESSORY -> 30;
            case GLASSES -> 40;
            case HAT -> 50;
            case PROFILE_FRAME -> 60;
            case BADGE -> 70;
        };
    }

    private static UUID parseUserId(String userIdText) {
        try {
            return UUID.fromString(userIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        }
    }

    private static DepartmentSummaryResponse toDepartmentSummary(UserAccount user) {
        if (user.departmentId() == null) {
            return null;
        }
        return new DepartmentSummaryResponse(
                user.departmentId(),
                user.departmentCode(),
                user.departmentDisplayName(),
                Boolean.TRUE.equals(user.departmentActive())
        );
    }
}
