package com.officearcade.server.profile.dto;

import com.officearcade.server.departments.dto.DepartmentSummaryResponse;
import java.util.List;

public record ProfileMeResponse(
        String userId,
        String displayName,
        String email,
        String role,
        boolean accountEnabled,
        DepartmentSummaryResponse department,
        int level,
        int xp,
        int respectPoints,
        int karmaPoints,
        int ownedCosmeticCount,
        int equippedCosmeticCount,
        List<ProfileOwnedCosmeticResponse> ownedCosmetics,
        List<ProfileEquippedCosmeticResponse> equippedCosmetics,
        List<ProfileAvatarLayerResponse> avatarLayers,
        String profileUpdatedAt,
        String generatedAt
) {
}
