package com.officearcade.server.lobby;

import com.officearcade.server.lobby.dto.CreateLobbyRoomRequest;
import com.officearcade.server.lobby.dto.JoinLobbyRoomRequest;
import com.officearcade.server.lobby.dto.LobbyGameTypeResponse;
import com.officearcade.server.lobby.dto.LobbyRoomActionResponse;
import com.officearcade.server.lobby.dto.LobbyRoomDetailResponse;
import com.officearcade.server.lobby.dto.LobbyRoomListResponse;
import com.officearcade.server.lobby.dto.MyLobbyRoomResponse;
import com.officearcade.server.security.OfficeArcadePrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/lobby")
@PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
public class LobbyController {

    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @GetMapping("/game-types")
    public List<LobbyGameTypeResponse> listEnabledGameTypes() {
        return lobbyService.listEnabledGameTypes();
    }

    @GetMapping("/rooms")
    public LobbyRoomListResponse listRooms(
            @RequestParam(required = false) String gameTypeCode,
            @RequestParam(required = false) Boolean isPrivate,
            @RequestParam(required = false) RoomStatus status
    ) {
        return lobbyService.listRooms(gameTypeCode, isPrivate, status);
    }

    @GetMapping("/rooms/{roomId}")
    public LobbyRoomDetailResponse getRoomById(@PathVariable String roomId) {
        return lobbyService.getRoomDetails(roomId);
    }

    @GetMapping("/my-room")
    public MyLobbyRoomResponse getMyRoom(@AuthenticationPrincipal OfficeArcadePrincipal principal) {
        return lobbyService.getMyRoom(getRequiredPrincipal(principal).id());
    }

    @PostMapping("/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public LobbyRoomDetailResponse createRoom(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @Valid @RequestBody CreateLobbyRoomRequest request
    ) {
        return lobbyService.createRoom(getRequiredPrincipal(principal).id(), request);
    }

    @PostMapping("/rooms/{roomId}/join")
    public LobbyRoomDetailResponse joinRoom(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId,
            @Valid @RequestBody(required = false) JoinLobbyRoomRequest request
    ) {
        JoinLobbyRoomRequest resolvedRequest = request == null ? new JoinLobbyRoomRequest(null) : request;
        return lobbyService.joinRoom(getRequiredPrincipal(principal).id(), roomId, resolvedRequest);
    }

    @PostMapping("/rooms/{roomId}/leave")
    public LobbyRoomActionResponse leaveRoom(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return lobbyService.leaveRoom(getRequiredPrincipal(principal).id(), roomId);
    }

    @PostMapping("/rooms/{roomId}/close")
    public LobbyRoomActionResponse closeRoom(
            @AuthenticationPrincipal OfficeArcadePrincipal principal,
            @PathVariable String roomId
    ) {
        return lobbyService.closeRoom(getRequiredPrincipal(principal).id(), roomId);
    }

    private static OfficeArcadePrincipal getRequiredPrincipal(OfficeArcadePrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication context.");
        }
        return principal;
    }
}
