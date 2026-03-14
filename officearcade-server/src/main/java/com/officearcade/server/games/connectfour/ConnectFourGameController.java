package com.officearcade.server.games.connectfour;

import com.officearcade.server.games.connectfour.dto.ConnectFourGameStateResponse;
import com.officearcade.server.games.connectfour.dto.ConnectFourMoveRequest;
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
@RequestMapping("/api/games/connect-four")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class ConnectFourGameController {

    private final ConnectFourGameService connectFourGameService;

    public ConnectFourGameController(ConnectFourGameService connectFourGameService) {
        this.connectFourGameService = connectFourGameService;
    }

    @GetMapping("/room/{roomId}")
    public ConnectFourGameStateResponse getRoomGameState(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return connectFourGameService.getGameState(roomId, requiredPrincipal(principal).id());
    }

    @PostMapping("/room/{roomId}/start")
    @ResponseStatus(HttpStatus.OK)
    public ConnectFourGameStateResponse startGame(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return connectFourGameService.startGame(roomId, requiredPrincipal(principal).id());
    }

    @PostMapping("/room/{roomId}/move")
    @ResponseStatus(HttpStatus.OK)
    public ConnectFourGameStateResponse submitMove(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId,
            @Valid @RequestBody ConnectFourMoveRequest request
    ) {
        return connectFourGameService.makeMove(roomId, requiredPrincipal(principal).id(), request.column());
    }

    private static OfficeArcadePrincipal requiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
