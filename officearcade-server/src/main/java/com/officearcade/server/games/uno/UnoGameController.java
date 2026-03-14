package com.officearcade.server.games.uno;

import com.officearcade.server.games.uno.dto.UnoGameStateResponse;
import com.officearcade.server.games.uno.dto.UnoPlayCardRequest;
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
@RequestMapping("/api/games/uno")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class UnoGameController {

    private final UnoGameService unoGameService;

    public UnoGameController(UnoGameService unoGameService) {
        this.unoGameService = unoGameService;
    }

    @GetMapping("/room/{roomId}")
    public UnoGameStateResponse getRoomGameState(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return unoGameService.getGameState(roomId, requiredPrincipal(principal).id());
    }

    @PostMapping("/room/{roomId}/start")
    @ResponseStatus(HttpStatus.OK)
    public UnoGameStateResponse startGame(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return unoGameService.startGame(roomId, requiredPrincipal(principal).id());
    }

    @PostMapping("/room/{roomId}/play")
    @ResponseStatus(HttpStatus.OK)
    public UnoGameStateResponse playCard(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId,
            @Valid @RequestBody UnoPlayCardRequest request
    ) {
        return unoGameService.playCard(roomId, requiredPrincipal(principal).id(), request.cardToken());
    }

    @PostMapping("/room/{roomId}/draw")
    @ResponseStatus(HttpStatus.OK)
    public UnoGameStateResponse drawCard(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return unoGameService.drawCard(roomId, requiredPrincipal(principal).id());
    }

    private static OfficeArcadePrincipal requiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
