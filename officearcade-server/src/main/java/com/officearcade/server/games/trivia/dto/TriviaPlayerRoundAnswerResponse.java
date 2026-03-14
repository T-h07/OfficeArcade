package com.officearcade.server.games.trivia.dto;

public record TriviaPlayerRoundAnswerResponse(
        String userId,
        String displayName,
        int selectedOptionIndex,
        boolean correct
) {
}
