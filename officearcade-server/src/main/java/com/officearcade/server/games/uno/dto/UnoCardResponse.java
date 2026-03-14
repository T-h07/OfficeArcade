package com.officearcade.server.games.uno.dto;

public record UnoCardResponse(
        String token,
        String color,
        String type,
        String label
) {
}
