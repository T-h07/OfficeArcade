package com.officearcade.server.games.connectfour.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ConnectFourMoveRequest(
        @NotNull(message = "Column is required.")
        @Min(value = 0, message = "Column must be between 0 and 6.")
        @Max(value = 6, message = "Column must be between 0 and 6.")
        Integer column
) {
}
