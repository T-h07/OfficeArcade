package com.officearcade.server.lobby.dto;

import java.util.List;

public record LobbyRoomListResponse(
        List<LobbyRoomSummaryResponse> rooms,
        int total
) {
}
