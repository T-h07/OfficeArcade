package com.officearcade.server.games.trivia.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record TriviaAnswerRequest(
        @Min(value = 0, message = "Trivia answer option index must be between 0 and 3.")
        @Max(value = 3, message = "Trivia answer option index must be between 0 and 3.")
        int selectedOptionIndex
) {
}
