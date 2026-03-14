package com.officearcade.server.games.uno.dto;

public record UnoPlayerResponse(
        String userId,
        String displayName,
        int handCount,
        boolean currentTurn
) {
}
