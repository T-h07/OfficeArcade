package com.officearcade.server.games.connectfour.dto;

import java.util.List;

public record ConnectFourGameStateResponse(
        String roomId,
        String gameSessionId,
        String status,
        int rows,
        int columns,
        int[][] board,
        List<ConnectFourPlayerResponse> players,
        String playerOneUserId,
        String playerTwoUserId,
        String currentTurnUserId,
        String winnerUserId,
        boolean draw,
        int moveCount,
        boolean canStart,
        boolean canMove,
        boolean myTurn,
        String startedAt,
        String endedAt,
        String updatedAt,
        ConnectFourChallengeSummaryResponse challenge
) {
}
