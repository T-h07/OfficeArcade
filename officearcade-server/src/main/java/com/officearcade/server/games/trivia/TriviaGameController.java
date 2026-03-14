package com.officearcade.server.games.trivia;

import com.officearcade.server.games.trivia.dto.TriviaAnswerRequest;
import com.officearcade.server.games.trivia.dto.TriviaGameStateResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games/trivia")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class TriviaGameController {

    private final TriviaGameService triviaGameService;

    public TriviaGameController(TriviaGameService triviaGameService) {
        this.triviaGameService = triviaGameService;
    }

    @GetMapping("/room/{roomId}")
    public TriviaGameStateResponse getRoomGameState(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return triviaGameService.getGameState(roomId, requiredPrincipal(principal).id());
    }

    @PostMapping("/room/{roomId}/start")
    @ResponseStatus(HttpStatus.OK)
    public TriviaGameStateResponse startGame(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return triviaGameService.startGame(roomId, requiredPrincipal(principal).id());
    }

    @PostMapping("/room/{roomId}/answer")
    @ResponseStatus(HttpStatus.OK)
    public TriviaGameStateResponse submitAnswer(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId,
            @Valid @RequestBody TriviaAnswerRequest request
    ) {
        return triviaGameService.submitAnswer(roomId, requiredPrincipal(principal).id(), request.selectedOptionIndex());
    }

    private static OfficeArcadePrincipal requiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
