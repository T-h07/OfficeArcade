package com.officearcade.server.games.uno.dto;

import java.util.List;

public record UnoGameStateResponse(
        String roomId,
        String gameSessionId,
        String status,
        List<UnoPlayerResponse> players,
        String currentTurnUserId,
        String winnerUserId,
        String direction,
        String currentColor,
        UnoCardResponse topDiscardCard,
        List<UnoCardResponse> myHand,
        List<String> playableCardTokens,
        int drawPileCount,
        int discardPileCount,
        int moveCount,
        boolean canStart,
        boolean canPlay,
        boolean canDraw,
        boolean myTurn,
        String startedAt,
        String endedAt,
        String updatedAt
) {
}
