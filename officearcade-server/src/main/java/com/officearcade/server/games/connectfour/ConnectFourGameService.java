package com.officearcade.server.games.connectfour;

import com.officearcade.server.games.connectfour.dto.ConnectFourGameStateResponse;
import com.officearcade.server.games.connectfour.dto.ConnectFourPlayerResponse;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntity;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntityRepository;
import com.officearcade.server.games.connectfour.realtime.ConnectFourRealtimeEventType;
import com.officearcade.server.games.connectfour.realtime.ConnectFourRealtimePublisher;
import com.officearcade.server.lobby.RoomMemberRole;
import com.officearcade.server.lobby.persistence.RoomEntity;
import com.officearcade.server.lobby.persistence.RoomEntityRepository;
import com.officearcade.server.lobby.persistence.RoomMemberEntity;
import com.officearcade.server.lobby.persistence.RoomMemberEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConnectFourGameService {

    private static final String CONNECT_FOUR_CODE = "CONNECT_FOUR";

    private final RoomEntityRepository roomEntityRepository;
    private final RoomMemberEntityRepository roomMemberEntityRepository;
    private final ConnectFourGameEntityRepository connectFourGameEntityRepository;
    private final ConnectFourRealtimePublisher connectFourRealtimePublisher;

    public ConnectFourGameService(
            RoomEntityRepository roomEntityRepository,
            RoomMemberEntityRepository roomMemberEntityRepository,
            ConnectFourGameEntityRepository connectFourGameEntityRepository,
            ConnectFourRealtimePublisher connectFourRealtimePublisher
    ) {
        this.roomEntityRepository = roomEntityRepository;
        this.roomMemberEntityRepository = roomMemberEntityRepository;
        this.connectFourGameEntityRepository = connectFourGameEntityRepository;
        this.connectFourRealtimePublisher = connectFourRealtimePublisher;
    }

    @Transactional(readOnly = true)
    public ConnectFourGameStateResponse getGameState(String roomIdText, String currentUserIdText) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredConnectFourRoom(roomId);
        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        assertUserIsRoomMember(members, currentUserId);

        Optional<ConnectFourGameEntity> session = connectFourGameEntityRepository.findByRoom_Id(roomId);
        return toGameStateResponse(room, members, session.orElse(null), currentUserId);
    }

    @Transactional
    public ConnectFourGameStateResponse startGame(String roomIdText, String currentUserIdText) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredConnectFourRoom(roomId);
        assertHostOnly(room, currentUserId);

        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        if (members.size() != 2) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Connect Four requires exactly 2 room members to start."
            );
        }

        UserEntity playerOne = members.get(0).getUser();
        UserEntity playerTwo = members.get(1).getUser();

        ConnectFourGameEntity game = connectFourGameEntityRepository.findByRoomIdForUpdate(roomId)
                .orElseGet(() -> {
                    ConnectFourGameEntity created = new ConnectFourGameEntity();
                    created.setRoom(room);
                    return created;
                });

        if (game.getStatus() == ConnectFourGameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Connect Four game is already active.");
        }

        int[][] emptyBoard = ConnectFourRulesEngine.emptyBoard();
        game.setStatus(ConnectFourGameStatus.ACTIVE);
        game.setPlayerOneUser(playerOne);
        game.setPlayerTwoUser(playerTwo);
        game.setCurrentTurnUser(playerOne);
        game.setWinnerUser(null);
        game.setDraw(false);
        game.setMoveCount(0);
        game.setBoardState(ConnectFourRulesEngine.encodeBoard(emptyBoard));
        game.setStartedAt(Instant.now());
        game.setEndedAt(null);

        ConnectFourGameEntity saved = connectFourGameEntityRepository.save(game);
        connectFourRealtimePublisher.publishGameEvent(
                ConnectFourRealtimeEventType.GAME_STARTED,
                roomId,
                saved.getId(),
                currentUserIdText
        );

        return toGameStateResponse(room, members, saved, currentUserId);
    }

    @Transactional
    public ConnectFourGameStateResponse makeMove(String roomIdText, String currentUserIdText, int column) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredConnectFourRoom(roomId);
        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        assertUserIsRoomMember(members, currentUserId);

        ConnectFourGameEntity game = connectFourGameEntityRepository.findByRoomIdForUpdate(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Connect Four game has not started yet."
                ));

        if (game.getStatus() != ConnectFourGameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Connect Four game is not active.");
        }
        if (game.getCurrentTurnUser() == null || !game.getCurrentTurnUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "It is not your turn.");
        }

        UUID playerOneId = getRequiredPlayerId(game.getPlayerOneUser(), "Player one is missing.");
        UUID playerTwoId = getRequiredPlayerId(game.getPlayerTwoUser(), "Player two is missing.");

        int token;
        UserEntity nextTurnUser;
        if (currentUserId.equals(playerOneId)) {
            token = ConnectFourRulesEngine.PLAYER_ONE_TOKEN;
            nextTurnUser = game.getPlayerTwoUser();
        } else if (currentUserId.equals(playerTwoId)) {
            token = ConnectFourRulesEngine.PLAYER_TWO_TOKEN;
            nextTurnUser = game.getPlayerOneUser();
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only match participants may play moves.");
        }

        int[][] board = ConnectFourRulesEngine.decodeBoard(game.getBoardState());
        int placedRow = ConnectFourRulesEngine.dropToken(board, column, token);
        if (placedRow < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected column is full.");
        }

        game.setMoveCount(game.getMoveCount() + 1);
        ConnectFourRealtimeEventType eventType = ConnectFourRealtimeEventType.MOVE_PLAYED;

        if (ConnectFourRulesEngine.hasWinningConnection(board, placedRow, column, token)) {
            game.setStatus(ConnectFourGameStatus.FINISHED);
            game.setWinnerUser(game.getCurrentTurnUser());
            game.setCurrentTurnUser(null);
            game.setDraw(false);
            game.setEndedAt(Instant.now());
            eventType = ConnectFourRealtimeEventType.GAME_FINISHED;
        } else if (ConnectFourRulesEngine.isBoardFull(board)) {
            game.setStatus(ConnectFourGameStatus.FINISHED);
            game.setWinnerUser(null);
            game.setCurrentTurnUser(null);
            game.setDraw(true);
            game.setEndedAt(Instant.now());
            eventType = ConnectFourRealtimeEventType.GAME_FINISHED;
        } else {
            game.setCurrentTurnUser(nextTurnUser);
        }

        game.setBoardState(ConnectFourRulesEngine.encodeBoard(board));
        ConnectFourGameEntity saved = connectFourGameEntityRepository.save(game);

        connectFourRealtimePublisher.publishGameEvent(
                eventType,
                roomId,
                saved.getId(),
                currentUserIdText
        );

        return toGameStateResponse(room, members, saved, currentUserId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveGame(UUID roomId) {
        return connectFourGameEntityRepository.existsByRoom_IdAndStatus(roomId, ConnectFourGameStatus.ACTIVE);
    }

    @Transactional
    public void handleRoomClosed(UUID roomId, String triggeredByUserId) {
        Optional<ConnectFourGameEntity> existing = connectFourGameEntityRepository.findByRoomIdForUpdate(roomId);
        if (existing.isEmpty()) {
            return;
        }

        ConnectFourGameEntity game = existing.get();
        if (game.getStatus() == ConnectFourGameStatus.FINISHED) {
            return;
        }

        game.setStatus(ConnectFourGameStatus.FINISHED);
        game.setCurrentTurnUser(null);
        game.setWinnerUser(null);
        game.setDraw(false);
        game.setEndedAt(Instant.now());
        ConnectFourGameEntity saved = connectFourGameEntityRepository.save(game);

        connectFourRealtimePublisher.publishGameEvent(
                ConnectFourRealtimeEventType.GAME_ABORTED,
                roomId,
                saved.getId(),
                triggeredByUserId
        );
    }

    private ConnectFourGameStateResponse toGameStateResponse(
            RoomEntity room,
            List<RoomMemberEntity> members,
            ConnectFourGameEntity game,
            UUID currentUserId
    ) {
        int[][] board;
        String gameSessionId = null;
        String status = ConnectFourGameStatus.WAITING.name();
        String playerOneUserId = null;
        String playerTwoUserId = null;
        String currentTurnUserId = null;
        String winnerUserId = null;
        boolean draw = false;
        int moveCount = 0;
        String startedAt = null;
        String endedAt = null;
        String updatedAt = room.getUpdatedAt().toString();

        List<ConnectFourPlayerResponse> players = members.stream()
                .map(member -> new ConnectFourPlayerResponse(
                        member.getUser().getId().toString(),
                        member.getUser().getDisplayName()
                ))
                .toList();

        if (game == null) {
            board = ConnectFourRulesEngine.emptyBoard();
            if (!players.isEmpty()) {
                playerOneUserId = players.get(0).userId();
            }
            if (players.size() > 1) {
                playerTwoUserId = players.get(1).userId();
            }
        } else {
            board = ConnectFourRulesEngine.decodeBoard(game.getBoardState());
            gameSessionId = game.getId().toString();
            status = game.getStatus().name();
            playerOneUserId = nullableUserId(game.getPlayerOneUser());
            playerTwoUserId = nullableUserId(game.getPlayerTwoUser());
            currentTurnUserId = nullableUserId(game.getCurrentTurnUser());
            winnerUserId = nullableUserId(game.getWinnerUser());
            draw = game.isDraw();
            moveCount = game.getMoveCount();
            startedAt = nullableInstant(game.getStartedAt());
            endedAt = nullableInstant(game.getEndedAt());
            updatedAt = game.getUpdatedAt().toString();
        }

        boolean currentUserIsHost = room.getHostUser().getId().equals(currentUserId);
        boolean hasTwoMembers = members.size() == 2;
        boolean canStart = currentUserIsHost
                && hasTwoMembers
                && (game == null || game.getStatus() == ConnectFourGameStatus.FINISHED || game.getStatus() == ConnectFourGameStatus.WAITING);

        boolean currentUserIsParticipant = currentUserId.toString().equals(playerOneUserId)
                || currentUserId.toString().equals(playerTwoUserId);
        boolean myTurn = currentTurnUserId != null && currentTurnUserId.equals(currentUserId.toString());
        boolean canMove = "ACTIVE".equals(status) && currentUserIsParticipant && myTurn;

        return new ConnectFourGameStateResponse(
                room.getId().toString(),
                gameSessionId,
                status,
                ConnectFourRulesEngine.ROWS,
                ConnectFourRulesEngine.COLUMNS,
                board,
                players,
                playerOneUserId,
                playerTwoUserId,
                currentTurnUserId,
                winnerUserId,
                draw,
                moveCount,
                canStart,
                canMove,
                myTurn,
                startedAt,
                endedAt,
                updatedAt
        );
    }

    private RoomEntity getRequiredConnectFourRoom(UUID roomId) {
        RoomEntity room = roomEntityRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomId));
        if (!CONNECT_FOUR_CODE.equalsIgnoreCase(room.getGameType().getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This room is not configured for Connect Four."
            );
        }
        return room;
    }

    private void assertHostOnly(RoomEntity room, UUID userId) {
        if (!room.getHostUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room host can start a Connect Four game.");
        }
    }

    private static void assertUserIsRoomMember(List<RoomMemberEntity> members, UUID userId) {
        boolean isMember = members.stream().anyMatch(member -> member.getUser().getId().equals(userId));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only room members can access this game.");
        }
    }

    private static UUID getRequiredPlayerId(UserEntity user, String message) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
        return user.getId();
    }

    private static String nullableUserId(UserEntity user) {
        return user == null ? null : user.getId().toString();
    }

    private static String nullableInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static UUID parseRoomId(String roomIdText) {
        try {
            return UUID.fromString(roomIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room id must be a valid UUID.");
        }
    }

    private static UUID parseUserId(String userIdText) {
        try {
            return UUID.fromString(userIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        }
    }
}
