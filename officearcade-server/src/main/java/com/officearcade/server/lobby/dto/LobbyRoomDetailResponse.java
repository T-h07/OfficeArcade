package com.officearcade.server.lobby.dto;

import java.util.List;

public record LobbyRoomDetailResponse(
        String id,
        String roomName,
        String hostUserId,
        String hostDisplayName,
        String gameTypeCode,
        String gameTypeDisplayName,
        boolean isPrivate,
        int currentPlayers,
        int maxPlayers,
        int rounds,
        String status,
        String createdAt,
        String updatedAt,
        List<LobbyRoomMemberResponse> members
) {
}
