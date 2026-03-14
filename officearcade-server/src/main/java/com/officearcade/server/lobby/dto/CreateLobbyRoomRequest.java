package com.officearcade.server.lobby.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLobbyRoomRequest(
        @NotBlank(message = "Room name is required.")
        @Size(min = 3, max = 80, message = "Room name must be between 3 and 80 characters.")
        String roomName,
        @NotBlank(message = "Game type code is required.")
        String gameTypeCode,
        @NotNull(message = "Max players is required.")
        @Min(value = 2, message = "Max players must be at least 2.")
        @Max(value = 8, message = "Max players cannot exceed 8.")
        Integer maxPlayers,
        @NotNull(message = "Rounds is required.")
        @Min(value = 1, message = "Rounds must be at least 1.")
        @Max(value = 10, message = "Rounds cannot exceed 10.")
        Integer rounds,
        @NotNull(message = "Room visibility is required.")
        Boolean isPrivate,
        @Size(max = 72, message = "Password cannot exceed 72 characters.")
        String password
) {
}
