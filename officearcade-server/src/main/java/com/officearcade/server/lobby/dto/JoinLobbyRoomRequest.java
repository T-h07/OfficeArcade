package com.officearcade.server.lobby.dto;

import jakarta.validation.constraints.Size;

public record JoinLobbyRoomRequest(
        @Size(max = 72, message = "Password cannot exceed 72 characters.")
        String password
) {
}
