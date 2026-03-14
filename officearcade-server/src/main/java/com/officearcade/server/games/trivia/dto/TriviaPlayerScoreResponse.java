package com.officearcade.server.games.trivia.dto;

public record TriviaPlayerScoreResponse(
        String userId,
        String displayName,
        int score,
        boolean answeredCurrentRound
) {
}
