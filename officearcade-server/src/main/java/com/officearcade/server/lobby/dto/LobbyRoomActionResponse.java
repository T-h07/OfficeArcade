package com.officearcade.server.lobby.dto;

public record LobbyRoomActionResponse(
        String status,
        String message,
        LobbyRoomDetailResponse room
) {
}
