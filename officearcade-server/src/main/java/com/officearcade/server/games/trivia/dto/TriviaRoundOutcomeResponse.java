package com.officearcade.server.games.trivia.dto;

import java.util.List;

public record TriviaRoundOutcomeResponse(
        int roundNumber,
        String questionId,
        String questionPrompt,
        int correctOptionIndex,
        List<TriviaPlayerRoundAnswerResponse> playerAnswers,
        String resolvedAt
) {
}
