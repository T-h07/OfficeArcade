package com.officearcade.server.games.trivia.dto;

import java.util.List;

public record TriviaQuestionResponse(
        String questionId,
        String prompt,
        String category,
        String difficulty,
        List<TriviaAnswerOptionResponse> options
) {
}
