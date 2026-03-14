package com.officearcade.server.games.uno;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.officearcade.server.games.uno.dto.UnoCardResponse;
import com.officearcade.server.games.uno.dto.UnoGameStateResponse;
import com.officearcade.server.games.uno.dto.UnoPlayerResponse;
import com.officearcade.server.games.uno.persistence.UnoGameEntity;
import com.officearcade.server.games.uno.persistence.UnoGameEntityRepository;
import com.officearcade.server.games.uno.realtime.UnoRealtimeEventType;
import com.officearcade.server.games.uno.realtime.UnoRealtimePublisher;
import com.officearcade.server.lobby.persistence.RoomEntity;
import com.officearcade.server.lobby.persistence.RoomEntityRepository;
import com.officearcade.server.lobby.persistence.RoomMemberEntity;
import com.officearcade.server.lobby.persistence.RoomMemberEntityRepository;
import com.officearcade.server.playlimits.PlayLimitService;
import com.officearcade.server.users.persistence.UserEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UnoGameService {

    private static final String UNO_CODE = "UNO";
    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 4;
    private static final int STARTING_HAND_SIZE = 7;
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<Map<String, List<String>>> STRING_LIST_MAP_TYPE = new TypeReference<>() {
    };

    private final RoomEntityRepository roomEntityRepository;
    private final RoomMemberEntityRepository roomMemberEntityRepository;
    private final UnoGameEntityRepository unoGameEntityRepository;
    private final UnoRealtimePublisher unoRealtimePublisher;
    private final PlayLimitService playLimitService;
    private final ObjectMapper objectMapper;

    public UnoGameService(
            RoomEntityRepository roomEntityRepository,
            RoomMemberEntityRepository roomMemberEntityRepository,
            UnoGameEntityRepository unoGameEntityRepository,
            UnoRealtimePublisher unoRealtimePublisher,
            PlayLimitService playLimitService,
            ObjectMapper objectMapper
    ) {
        this.roomEntityRepository = roomEntityRepository;
        this.roomMemberEntityRepository = roomMemberEntityRepository;
        this.unoGameEntityRepository = unoGameEntityRepository;
        this.unoRealtimePublisher = unoRealtimePublisher;
        this.playLimitService = playLimitService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public UnoGameStateResponse getGameState(String roomIdText, String currentUserIdText) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredUnoRoom(roomId);
        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        assertUserIsRoomMember(members, currentUserId);

        Optional<UnoGameEntity> game = unoGameEntityRepository.findByRoom_Id(roomId);
        return toGameStateResponse(room, members, game.orElse(null), currentUserId);
    }

    @Transactional
    public UnoGameStateResponse startGame(String roomIdText, String currentUserIdText) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredUnoRoom(roomId);
        assertHostOnly(room, currentUserId);

        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        validatePlayerCount(members.size());

        for (RoomMemberEntity member : members) {
            playLimitService.assertEligibleForPlayableGame(member.getUser().getId(), UNO_CODE);
        }

        UnoGameEntity game = unoGameEntityRepository.findByRoomIdForUpdate(roomId)
                .orElseGet(() -> {
                    UnoGameEntity created = new UnoGameEntity();
                    created.setRoom(room);
                    return created;
                });

        if (game.getStatus() == UnoGameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UNO game is already active for this room.");
        }

        List<UUID> orderedPlayerIds = members.stream()
                .map(member -> member.getUser().getId())
                .toList();

        List<String> drawPile = buildShuffledDeck();
        List<String> noDiscardBuffer = new ArrayList<>();
        Map<String, List<String>> hands = new LinkedHashMap<>();
        for (UUID playerId : orderedPlayerIds) {
            hands.put(playerId.toString(), new ArrayList<>());
        }

        for (int i = 0; i < STARTING_HAND_SIZE; i++) {
            for (UUID playerId : orderedPlayerIds) {
                hands.get(playerId.toString()).add(drawSingleCard(drawPile, noDiscardBuffer));
            }
        }

        List<String> deferredActionCards = new ArrayList<>();
        String startingCardToken = null;
        while (!drawPile.isEmpty()) {
            String candidate = drawSingleCard(drawPile, noDiscardBuffer);
            UnoCard candidateCard = parseCardToken(candidate);
            if (candidateCard.type() == UnoCardType.NUMBER) {
                startingCardToken = candidateCard.token();
                break;
            }
            deferredActionCards.add(candidateCard.token());
        }

        if (startingCardToken == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to initialize UNO discard pile."
            );
        }

        if (!deferredActionCards.isEmpty()) {
            drawPile.addAll(deferredActionCards);
            Collections.shuffle(drawPile);
        }

        UnoCard startingCard = parseCardToken(startingCardToken);
        List<String> discardPile = new ArrayList<>();
        discardPile.add(startingCard.token());

        game.setStatus(UnoGameStatus.ACTIVE);
        game.setPlayerOrderState(writeJson(orderedPlayerIds.stream().map(UUID::toString).toList(), "player order"));
        game.setCurrentTurnIndex(0);
        game.setDirection(1);
        game.setCurrentColor(startingCard.color().name());
        game.setDrawPileState(writeJson(drawPile, "draw pile"));
        game.setDiscardPileState(writeJson(discardPile, "discard pile"));
        game.setHandsState(writeJson(hands, "hands"));
        game.setWinnerUser(null);
        game.setMoveCount(0);
        game.setPlayLimitsApplied(false);
        game.setStartedAt(Instant.now());
        game.setEndedAt(null);

        UnoGameEntity saved = unoGameEntityRepository.save(game);
        unoRealtimePublisher.publishGameEvent(
                UnoRealtimeEventType.GAME_STARTED,
                roomId,
                saved.getId(),
                currentUserIdText
        );

        return toGameStateResponse(room, members, saved, currentUserId);
    }

    @Transactional
    public UnoGameStateResponse playCard(String roomIdText, String currentUserIdText, String cardTokenText) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);
        UnoCard playedCard = parseCardToken(requiredCardToken(cardTokenText));

        RoomEntity room = getRequiredUnoRoom(roomId);
        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        assertUserIsRoomMember(members, currentUserId);

        UnoGameEntity game = unoGameEntityRepository.findByRoomIdForUpdate(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "UNO game has not started yet."
                ));

        if (game.getStatus() != UnoGameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UNO game is not active.");
        }

        ParsedUnoState state = parseGameState(game);
        int playerCount = state.playerOrder().size();
        if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UNO game player order is invalid.");
        }

        int currentTurnIndex = normalizeTurnIndex(game.getCurrentTurnIndex(), playerCount);
        UUID currentTurnUserId = state.playerOrder().get(currentTurnIndex);
        if (!currentTurnUserId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "It is not your turn.");
        }

        List<String> currentHand = state.hands().get(currentUserId.toString());
        if (currentHand == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only active UNO participants can play cards.");
        }

        int handCardIndex = currentHand.indexOf(playedCard.token());
        if (handCardIndex < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Selected card is not in your hand.");
        }

        UnoCard topDiscardCard = parseTopDiscardCard(state.discardPile());
        UnoCardColor currentColor = resolveCurrentColor(game.getCurrentColor(), topDiscardCard);
        if (!isPlayable(playedCard, topDiscardCard, currentColor)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Selected card is not valid for the current top discard and color."
            );
        }

        currentHand.remove(handCardIndex);
        state.discardPile().add(playedCard.token());
        game.setCurrentColor(playedCard.color().name());
        game.setMoveCount(game.getMoveCount() + 1);

        UnoRealtimeEventType eventType = UnoRealtimeEventType.CARD_PLAYED;

        Map<UUID, RoomMemberEntity> membersById = membersByUserId(members);
        if (currentHand.isEmpty()) {
            game.setStatus(UnoGameStatus.FINISHED);
            game.setWinnerUser(requiredMemberUser(membersById, currentUserId));
            game.setEndedAt(Instant.now());
            eventType = UnoRealtimeEventType.GAME_FINISHED;
        } else {
            int direction = normalizeDirection(game.getDirection());
            int stepCount = 1;

            if (playedCard.type() == UnoCardType.SKIP) {
                stepCount = 2;
            } else if (playedCard.type() == UnoCardType.REVERSE) {
                direction = -direction;
                game.setDirection(direction);
                stepCount = playerCount == 2 ? 2 : 1;
            } else if (playedCard.type() == UnoCardType.DRAW_TWO) {
                int drawTargetIndex = advanceIndex(currentTurnIndex, direction, 1, playerCount);
                UUID drawTargetUserId = state.playerOrder().get(drawTargetIndex);
                List<String> drawTargetHand = state.hands()
                        .computeIfAbsent(drawTargetUserId.toString(), ignored -> new ArrayList<>());
                drawCards(drawTargetHand, state.drawPile(), state.discardPile(), 2);
                stepCount = 2;
            }

            game.setDirection(direction);
            game.setCurrentTurnIndex(advanceIndex(currentTurnIndex, direction, stepCount, playerCount));
        }

        persistMutableState(game, state);
        UnoGameEntity saved = unoGameEntityRepository.save(game);

        if (saved.getStatus() == UnoGameStatus.FINISHED && !saved.isPlayLimitsApplied()) {
            playLimitService.recordCompletedMatchForUsers(state.playerOrder());
            saved.setPlayLimitsApplied(true);
            saved = unoGameEntityRepository.save(saved);
        }

        unoRealtimePublisher.publishGameEvent(
                eventType,
                roomId,
                saved.getId(),
                currentUserIdText
        );

        return toGameStateResponse(room, members, saved, currentUserId);
    }

    @Transactional
    public UnoGameStateResponse drawCard(String roomIdText, String currentUserIdText) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredUnoRoom(roomId);
        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        assertUserIsRoomMember(members, currentUserId);

        UnoGameEntity game = unoGameEntityRepository.findByRoomIdForUpdate(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "UNO game has not started yet."
                ));

        if (game.getStatus() != UnoGameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UNO game is not active.");
        }

        ParsedUnoState state = parseGameState(game);
        int playerCount = state.playerOrder().size();
        if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "UNO game player order is invalid.");
        }

        int currentTurnIndex = normalizeTurnIndex(game.getCurrentTurnIndex(), playerCount);
        UUID currentTurnUserId = state.playerOrder().get(currentTurnIndex);
        if (!currentTurnUserId.equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "It is not your turn.");
        }

        List<String> currentHand = state.hands().get(currentUserId.toString());
        if (currentHand == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only active UNO participants can draw cards.");
        }

        UnoCard topDiscardCard = parseTopDiscardCard(state.discardPile());
        UnoCardColor currentColor = resolveCurrentColor(game.getCurrentColor(), topDiscardCard);
        if (hasPlayableCard(currentHand, topDiscardCard, currentColor)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already have a playable card and cannot draw this turn."
            );
        }

        currentHand.add(drawSingleCard(state.drawPile(), state.discardPile()));

        game.setMoveCount(game.getMoveCount() + 1);
        int direction = normalizeDirection(game.getDirection());
        game.setDirection(direction);
        game.setCurrentTurnIndex(advanceIndex(currentTurnIndex, direction, 1, playerCount));

        persistMutableState(game, state);
        UnoGameEntity saved = unoGameEntityRepository.save(game);

        unoRealtimePublisher.publishGameEvent(
                UnoRealtimeEventType.CARD_DRAWN,
                roomId,
                saved.getId(),
                currentUserIdText
        );

        return toGameStateResponse(room, members, saved, currentUserId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveGame(UUID roomId) {
        return unoGameEntityRepository.existsByRoom_IdAndStatus(roomId, UnoGameStatus.ACTIVE);
    }

    @Transactional
    public void handleRoomClosed(UUID roomId, String triggeredByUserId) {
        Optional<UnoGameEntity> existing = unoGameEntityRepository.findByRoomIdForUpdate(roomId);
        if (existing.isEmpty()) {
            return;
        }

        UnoGameEntity game = existing.get();
        if (game.getStatus() == UnoGameStatus.FINISHED) {
            return;
        }

        game.setStatus(UnoGameStatus.FINISHED);
        game.setWinnerUser(null);
        game.setEndedAt(Instant.now());
        UnoGameEntity saved = unoGameEntityRepository.save(game);

        unoRealtimePublisher.publishGameEvent(
                UnoRealtimeEventType.GAME_ABORTED,
                roomId,
                saved.getId(),
                triggeredByUserId
        );
    }

    private UnoGameStateResponse toGameStateResponse(
            RoomEntity room,
            List<RoomMemberEntity> members,
            UnoGameEntity game,
            UUID currentUserId
    ) {
        boolean currentUserIsHost = room.getHostUser().getId().equals(currentUserId);
        boolean canStartByMembership = members.size() >= MIN_PLAYERS && members.size() <= MAX_PLAYERS;

        if (game == null) {
            List<UnoPlayerResponse> waitingPlayers = members.stream()
                    .map(member -> new UnoPlayerResponse(
                            member.getUser().getId().toString(),
                            member.getUser().getDisplayName(),
                            0,
                            false
                    ))
                    .toList();

            return new UnoGameStateResponse(
                    room.getId().toString(),
                    null,
                    UnoGameStatus.WAITING.name(),
                    waitingPlayers,
                    null,
                    null,
                    "CLOCKWISE",
                    null,
                    null,
                    List.of(),
                    List.of(),
                    0,
                    0,
                    0,
                    currentUserIsHost && canStartByMembership,
                    false,
                    false,
                    false,
                    null,
                    null,
                    room.getUpdatedAt().toString()
            );
        }

        ParsedUnoState parsedState = parseGameState(game);
        Map<UUID, RoomMemberEntity> memberById = membersByUserId(members);

        int playerCount = parsedState.playerOrder().size();
        int safeTurnIndex = playerCount == 0 ? 0 : normalizeTurnIndex(game.getCurrentTurnIndex(), playerCount);
        UUID currentTurnUserId = game.getStatus() == UnoGameStatus.ACTIVE && playerCount > 0
                ? parsedState.playerOrder().get(safeTurnIndex)
                : null;

        List<UnoPlayerResponse> players = new ArrayList<>();
        for (UUID playerId : parsedState.playerOrder()) {
            RoomMemberEntity member = memberById.get(playerId);
            String displayName = member == null ? "Former member" : member.getUser().getDisplayName();
            int handCount = parsedState.hands().getOrDefault(playerId.toString(), List.of()).size();
            players.add(new UnoPlayerResponse(
                    playerId.toString(),
                    displayName,
                    handCount,
                    currentTurnUserId != null && currentTurnUserId.equals(playerId)
            ));
        }

        List<String> myHandTokens = parsedState.hands().getOrDefault(currentUserId.toString(), List.of());
        List<UnoCardResponse> myHand = myHandTokens.stream().map(token -> toCardResponse(parseCardToken(token))).toList();

        UnoCard topDiscardCard = parsedState.discardPile().isEmpty() ? null : parseCardToken(parsedState.discardPile().getLast());
        UnoCardColor currentColor = topDiscardCard == null ? null : resolveCurrentColor(game.getCurrentColor(), topDiscardCard);

        boolean myTurn = currentTurnUserId != null && currentTurnUserId.equals(currentUserId);
        List<String> playableCardTokens = new ArrayList<>();
        if (myTurn && topDiscardCard != null && currentColor != null) {
            for (String token : myHandTokens) {
                UnoCard candidate = parseCardToken(token);
                if (isPlayable(candidate, topDiscardCard, currentColor)) {
                    playableCardTokens.add(candidate.token());
                }
            }
        }

        boolean canPlay = game.getStatus() == UnoGameStatus.ACTIVE && myTurn && !playableCardTokens.isEmpty();
        boolean canDraw = game.getStatus() == UnoGameStatus.ACTIVE && myTurn && playableCardTokens.isEmpty();

        String directionLabel = normalizeDirection(game.getDirection()) == -1 ? "COUNTERCLOCKWISE" : "CLOCKWISE";

        return new UnoGameStateResponse(
                room.getId().toString(),
                game.getId().toString(),
                game.getStatus().name(),
                players,
                currentTurnUserId == null ? null : currentTurnUserId.toString(),
                game.getWinnerUser() == null ? null : game.getWinnerUser().getId().toString(),
                directionLabel,
                currentColor == null ? null : currentColor.name(),
                topDiscardCard == null ? null : toCardResponse(topDiscardCard),
                myHand,
                playableCardTokens,
                parsedState.drawPile().size(),
                parsedState.discardPile().size(),
                game.getMoveCount(),
                currentUserIsHost && canStartByMembership && game.getStatus() != UnoGameStatus.ACTIVE,
                canPlay,
                canDraw,
                myTurn,
                nullableInstant(game.getStartedAt()),
                nullableInstant(game.getEndedAt()),
                game.getUpdatedAt().toString()
        );
    }

    private static Map<UUID, RoomMemberEntity> membersByUserId(List<RoomMemberEntity> members) {
        Map<UUID, RoomMemberEntity> mapped = new LinkedHashMap<>();
        for (RoomMemberEntity member : members) {
            mapped.put(member.getUser().getId(), member);
        }
        return mapped;
    }

    private UserEntity requiredMemberUser(Map<UUID, RoomMemberEntity> membersById, UUID userId) {
        RoomMemberEntity member = membersById.get(userId);
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "UNO winner is not a room member.");
        }
        return member.getUser();
    }

    private ParsedUnoState parseGameState(UnoGameEntity game) {
        List<String> playerIdTokens = parseStringList(game.getPlayerOrderState(), "player order");
        List<UUID> playerOrder = new ArrayList<>(playerIdTokens.size());
        for (String playerIdToken : playerIdTokens) {
            try {
                playerOrder.add(UUID.fromString(playerIdToken));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "UNO persisted player order contains an invalid user id."
                );
            }
        }

        Map<String, List<String>> persistedHands = parseStringListMap(game.getHandsState(), "hands");
        Map<String, List<String>> mutableHands = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : persistedHands.entrySet()) {
            mutableHands.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        for (UUID playerId : playerOrder) {
            mutableHands.computeIfAbsent(playerId.toString(), ignored -> new ArrayList<>());
        }

        List<String> drawPile = new ArrayList<>(parseStringList(game.getDrawPileState(), "draw pile"));
        List<String> discardPile = new ArrayList<>(parseStringList(game.getDiscardPileState(), "discard pile"));

        return new ParsedUnoState(playerOrder, drawPile, discardPile, mutableHands);
    }

    private void persistMutableState(UnoGameEntity game, ParsedUnoState state) {
        game.setPlayerOrderState(writeJson(state.playerOrder().stream().map(UUID::toString).toList(), "player order"));
        game.setDrawPileState(writeJson(state.drawPile(), "draw pile"));
        game.setDiscardPileState(writeJson(state.discardPile(), "discard pile"));
        game.setHandsState(writeJson(state.hands(), "hands"));
    }

    private List<String> buildShuffledDeck() {
        List<String> deck = new ArrayList<>(100);
        for (UnoCardColor color : UnoCardColor.values()) {
            deck.add(token(color, "0"));
            for (int number = 1; number <= 9; number++) {
                String numericValue = String.valueOf(number);
                deck.add(token(color, numericValue));
                deck.add(token(color, numericValue));
            }
            deck.add(token(color, "SKIP"));
            deck.add(token(color, "SKIP"));
            deck.add(token(color, "REVERSE"));
            deck.add(token(color, "REVERSE"));
            deck.add(token(color, "DRAW_TWO"));
            deck.add(token(color, "DRAW_TWO"));
        }
        Collections.shuffle(deck);
        return deck;
    }

    private static void drawCards(
            List<String> targetHand,
            List<String> drawPile,
            List<String> discardPile,
            int count
    ) {
        for (int i = 0; i < count; i++) {
            targetHand.add(drawSingleCard(drawPile, discardPile));
        }
    }

    private static String drawSingleCard(List<String> drawPile, List<String> discardPile) {
        ensureDrawPileHasCards(drawPile, discardPile);
        if (drawPile.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No cards are left in the draw pile."
            );
        }
        return drawPile.remove(drawPile.size() - 1);
    }

    private static void ensureDrawPileHasCards(List<String> drawPile, List<String> discardPile) {
        if (!drawPile.isEmpty()) {
            return;
        }
        if (discardPile.size() <= 1) {
            return;
        }

        String topDiscard = discardPile.remove(discardPile.size() - 1);
        List<String> recycled = new ArrayList<>(discardPile);
        Collections.shuffle(recycled);
        discardPile.clear();
        discardPile.add(topDiscard);
        drawPile.addAll(recycled);
    }

    private static boolean hasPlayableCard(List<String> handTokens, UnoCard topDiscardCard, UnoCardColor currentColor) {
        for (String token : handTokens) {
            if (isPlayable(parseCardToken(token), topDiscardCard, currentColor)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isPlayable(UnoCard candidate, UnoCard topDiscardCard, UnoCardColor currentColor) {
        return candidate.color() == currentColor || candidate.valueToken().equals(topDiscardCard.valueToken());
    }

    private static UnoCard parseTopDiscardCard(List<String> discardPile) {
        if (discardPile.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UNO discard pile is empty."
            );
        }
        return parseCardToken(discardPile.get(discardPile.size() - 1));
    }

    private static UnoCardColor resolveCurrentColor(String storedColor, UnoCard topDiscardCard) {
        if (storedColor == null || storedColor.isBlank()) {
            return topDiscardCard.color();
        }

        try {
            return UnoCardColor.valueOf(storedColor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UNO current color is invalid."
            );
        }
    }

    private static int normalizeTurnIndex(int turnIndex, int playerCount) {
        if (playerCount <= 0) {
            return 0;
        }
        int normalized = turnIndex % playerCount;
        if (normalized < 0) {
            normalized += playerCount;
        }
        return normalized;
    }

    private static int advanceIndex(int currentIndex, int direction, int steps, int playerCount) {
        if (playerCount <= 0) {
            return 0;
        }
        int offset = (direction * steps) % playerCount;
        int next = currentIndex + offset;
        next %= playerCount;
        if (next < 0) {
            next += playerCount;
        }
        return next;
    }

    private static int normalizeDirection(int direction) {
        return direction == -1 ? -1 : 1;
    }

    private static UnoCardResponse toCardResponse(UnoCard card) {
        return new UnoCardResponse(
                card.token(),
                card.color().name(),
                card.type().name(),
                card.label()
        );
    }

    private static UnoCard parseCardToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNO card token is required.");
        }

        String normalized = token.trim().toUpperCase(Locale.ROOT);
        String[] parts = normalized.split(":");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNO card token format is invalid.");
        }

        UnoCardColor color;
        try {
            color = UnoCardColor.valueOf(parts[0]);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNO card color is invalid.");
        }

        String value = parts[1].trim();
        if (value.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNO card value is invalid.");
        }

        if (value.chars().allMatch(Character::isDigit)) {
            int numericValue;
            try {
                numericValue = Integer.parseInt(value);
            } catch (NumberFormatException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNO card number is invalid.");
            }
            if (numericValue < 0 || numericValue > 9) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNO card number must be between 0 and 9.");
            }
            return new UnoCard(color, UnoCardType.NUMBER, value, token(color, value), value);
        }

        return switch (value) {
            case "SKIP" -> new UnoCard(color, UnoCardType.SKIP, value, token(color, value), "Skip");
            case "REVERSE" -> new UnoCard(color, UnoCardType.REVERSE, value, token(color, value), "Reverse");
            case "DRAW_TWO" -> new UnoCard(color, UnoCardType.DRAW_TWO, value, token(color, value), "Draw Two");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UNO card action is invalid.");
        };
    }

    private static String requiredCardToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Card token is required.");
        }
        return token;
    }

    private static String token(UnoCardColor color, String value) {
        return color.name() + ":" + value;
    }

    private String writeJson(Object value, String context) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to serialize UNO " + context + " state."
            );
        }
    }

    private List<String> parseStringList(String value, String context) {
        try {
            if (value == null || value.isBlank()) {
                return List.of();
            }
            return objectMapper.readValue(value, STRING_LIST_TYPE);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UNO " + context + " state is malformed."
            );
        }
    }

    private Map<String, List<String>> parseStringListMap(String value, String context) {
        try {
            if (value == null || value.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(value, STRING_LIST_MAP_TYPE);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "UNO " + context + " state is malformed."
            );
        }
    }

    private RoomEntity getRequiredUnoRoom(UUID roomId) {
        RoomEntity room = roomEntityRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomId));
        if (!UNO_CODE.equalsIgnoreCase(room.getGameType().getCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This room is not configured for UNO."
            );
        }
        return room;
    }

    private static void assertHostOnly(RoomEntity room, UUID userId) {
        if (!room.getHostUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room host can start a UNO game.");
        }
    }

    private static void assertUserIsRoomMember(List<RoomMemberEntity> members, UUID userId) {
        boolean isMember = members.stream().anyMatch(member -> member.getUser().getId().equals(userId));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only room members can access this game.");
        }
    }

    private static void validatePlayerCount(int playerCount) {
        if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "UNO requires between 2 and 4 room members to start."
            );
        }
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

    private record UnoCard(
            UnoCardColor color,
            UnoCardType type,
            String valueToken,
            String token,
            String label
    ) {
    }

    private record ParsedUnoState(
            List<UUID> playerOrder,
            List<String> drawPile,
            List<String> discardPile,
            Map<String, List<String>> hands
    ) {
    }
}
