package com.officearcade.server.games.uno.dto;

import jakarta.validation.constraints.NotBlank;

public record UnoPlayCardRequest(
        @NotBlank(message = "Card token is required.")
        String cardToken
) {
}
