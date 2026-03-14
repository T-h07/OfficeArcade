package com.officearcade.server.lobby.dto;

public record LobbyRoomSummaryResponse(
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
        String updatedAt
) {
}
