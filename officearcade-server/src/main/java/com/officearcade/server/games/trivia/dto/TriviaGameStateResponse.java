package com.officearcade.server.games.trivia.dto;

import java.util.List;

public record TriviaGameStateResponse(
        String roomId,
        String gameSessionId,
        String status,
        int currentRound,
        int totalRounds,
        TriviaQuestionResponse currentQuestion,
        List<TriviaPlayerScoreResponse> players,
        List<String> submittedUserIds,
        String winnerUserId,
        boolean draw,
        boolean canStart,
        boolean canAnswer,
        boolean answeredByCurrentUser,
        boolean waitingForOpponent,
        TriviaRoundOutcomeResponse lastRoundOutcome,
        String startedAt,
        String endedAt,
        String updatedAt
) {
}
