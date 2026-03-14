package com.officearcade.server.lobby.dto;

public record LobbyRoomMemberResponse(
        String userId,
        String displayName,
        String role,
        String joinedAt
) {
}
