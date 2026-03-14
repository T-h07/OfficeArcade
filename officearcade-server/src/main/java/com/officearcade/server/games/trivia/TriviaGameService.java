package com.officearcade.server.games.trivia;

import com.officearcade.server.games.trivia.dto.TriviaAnswerOptionResponse;
import com.officearcade.server.games.trivia.dto.TriviaGameStateResponse;
import com.officearcade.server.games.trivia.dto.TriviaPlayerRoundAnswerResponse;
import com.officearcade.server.games.trivia.dto.TriviaPlayerScoreResponse;
import com.officearcade.server.games.trivia.dto.TriviaQuestionResponse;
import com.officearcade.server.games.trivia.dto.TriviaRoundOutcomeResponse;
import com.officearcade.server.games.trivia.persistence.TriviaGameEntity;
import com.officearcade.server.games.trivia.persistence.TriviaGameEntityRepository;
import com.officearcade.server.games.trivia.persistence.TriviaQuestionEntity;
import com.officearcade.server.games.trivia.persistence.TriviaQuestionEntityRepository;
import com.officearcade.server.games.trivia.persistence.TriviaRoundAnswerEntity;
import com.officearcade.server.games.trivia.persistence.TriviaRoundAnswerEntityRepository;
import com.officearcade.server.games.trivia.realtime.TriviaRealtimeEventType;
import com.officearcade.server.games.trivia.realtime.TriviaRealtimePublisher;
import com.officearcade.server.lobby.persistence.RoomEntity;
import com.officearcade.server.lobby.persistence.RoomEntityRepository;
import com.officearcade.server.lobby.persistence.RoomMemberEntity;
import com.officearcade.server.lobby.persistence.RoomMemberEntityRepository;
import com.officearcade.server.playlimits.PlayLimitService;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TriviaGameService {

    private static final String TRIVIA_CODE = "TRIVIA";
    private static final int REQUIRED_PLAYERS = 2;
    private static final int XP_PER_LEVEL = 250;
    private static final int XP_WIN = 60;
    private static final int XP_LOSS = 35;
    private static final int XP_DRAW = 40;

    private final RoomEntityRepository roomEntityRepository;
    private final RoomMemberEntityRepository roomMemberEntityRepository;
    private final TriviaGameEntityRepository triviaGameEntityRepository;
    private final TriviaQuestionEntityRepository triviaQuestionEntityRepository;
    private final TriviaRoundAnswerEntityRepository triviaRoundAnswerEntityRepository;
    private final TriviaRealtimePublisher triviaRealtimePublisher;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final PlayLimitService playLimitService;

    public TriviaGameService(
            RoomEntityRepository roomEntityRepository,
            RoomMemberEntityRepository roomMemberEntityRepository,
            TriviaGameEntityRepository triviaGameEntityRepository,
            TriviaQuestionEntityRepository triviaQuestionEntityRepository,
            TriviaRoundAnswerEntityRepository triviaRoundAnswerEntityRepository,
            TriviaRealtimePublisher triviaRealtimePublisher,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            PlayLimitService playLimitService
    ) {
        this.roomEntityRepository = roomEntityRepository;
        this.roomMemberEntityRepository = roomMemberEntityRepository;
        this.triviaGameEntityRepository = triviaGameEntityRepository;
        this.triviaQuestionEntityRepository = triviaQuestionEntityRepository;
        this.triviaRoundAnswerEntityRepository = triviaRoundAnswerEntityRepository;
        this.triviaRealtimePublisher = triviaRealtimePublisher;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.playLimitService = playLimitService;
    }

    @Transactional(readOnly = true)
    public TriviaGameStateResponse getGameState(String roomIdText, String currentUserIdText) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredTriviaRoom(roomId);
        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        assertUserIsRoomMember(members, currentUserId);

        Optional<TriviaGameEntity> game = triviaGameEntityRepository.findByRoom_Id(roomId);
        return toGameStateResponse(room, members, game.orElse(null), currentUserId);
    }

    @Transactional
    public TriviaGameStateResponse startGame(String roomIdText, String currentUserIdText) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredTriviaRoom(roomId);
        assertHostOnly(room, currentUserId);

        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        if (members.size() != REQUIRED_PLAYERS) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Trivia Battle requires exactly 2 room members to start."
            );
        }

        for (RoomMemberEntity member : members) {
            playLimitService.assertEligibleForPlayableGame(member.getUser().getId(), TRIVIA_CODE);
        }

        int totalRounds = room.getRounds();
        List<TriviaQuestionEntity> enabledQuestions = new ArrayList<>(
                triviaQuestionEntityRepository.findAllByEnabledTrueOrderByCodeAsc()
        );
        if (enabledQuestions.size() < totalRounds) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Not enough enabled trivia questions to start this match."
            );
        }
        Collections.shuffle(enabledQuestions);

        List<UUID> selectedQuestionIds = enabledQuestions.stream()
                .limit(totalRounds)
                .map(TriviaQuestionEntity::getId)
                .toList();

        TriviaQuestionEntity firstQuestion = enabledQuestions.getFirst();

        UserEntity playerOne = members.get(0).getUser();
        UserEntity playerTwo = members.get(1).getUser();

        TriviaGameEntity game = triviaGameEntityRepository.findByRoomIdForUpdate(roomId)
                .orElseGet(() -> {
                    TriviaGameEntity created = new TriviaGameEntity();
                    created.setRoom(room);
                    return created;
                });

        if (game.getStatus() == TriviaGameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trivia Battle is already active for this room.");
        }

        game.setStatus(TriviaGameStatus.ACTIVE);
        game.setPlayerOneUser(playerOne);
        game.setPlayerTwoUser(playerTwo);
        game.setCurrentRound(1);
        game.setTotalRounds(totalRounds);
        game.setQuestionSequence(encodeQuestionSequence(selectedQuestionIds));
        game.setCurrentQuestion(firstQuestion);
        game.setLastResolvedRound(null);
        game.setLastQuestion(null);
        game.setPlayerOneScore(0);
        game.setPlayerTwoScore(0);
        game.setWinnerUser(null);
        game.setDraw(false);
        game.setPlayLimitsApplied(false);
        game.setStartedAt(Instant.now());
        game.setEndedAt(null);

        TriviaGameEntity saved = triviaGameEntityRepository.save(game);
        triviaRoundAnswerEntityRepository.deleteAllByGame_Id(saved.getId());

        triviaRealtimePublisher.publishGameEvent(
                TriviaRealtimeEventType.GAME_STARTED,
                roomId,
                saved.getId(),
                currentUserIdText
        );

        return toGameStateResponse(room, members, saved, currentUserId);
    }

    @Transactional
    public TriviaGameStateResponse submitAnswer(String roomIdText, String currentUserIdText, int selectedOptionIndex) {
        UUID roomId = parseRoomId(roomIdText);
        UUID currentUserId = parseUserId(currentUserIdText);

        RoomEntity room = getRequiredTriviaRoom(roomId);
        List<RoomMemberEntity> members = roomMemberEntityRepository.findAllByRoom_IdOrderByJoinedAtAsc(roomId);
        assertUserIsRoomMember(members, currentUserId);

        TriviaGameEntity game = triviaGameEntityRepository.findByRoomIdForUpdate(roomId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Trivia Battle has not started yet."
                ));

        if (game.getStatus() != TriviaGameStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trivia Battle is not active.");
        }
        if (selectedOptionIndex < 0 || selectedOptionIndex > 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Answer option index must be between 0 and 3.");
        }

        UserEntity playerOne = requireUser(game.getPlayerOneUser(), "Trivia player one is missing.");
        UserEntity playerTwo = requireUser(game.getPlayerTwoUser(), "Trivia player two is missing.");
        boolean isParticipant = playerOne.getId().equals(currentUserId) || playerTwo.getId().equals(currentUserId);
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only trivia participants can submit answers.");
        }

        int currentRound = game.getCurrentRound();
        if (currentRound <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Trivia Battle round is not ready yet.");
        }

        TriviaQuestionEntity currentQuestion = game.getCurrentQuestion();
        if (currentQuestion == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Current trivia question is unavailable. Start a new match."
            );
        }

        Optional<TriviaRoundAnswerEntity> existingAnswer = triviaRoundAnswerEntityRepository
                .findByGame_IdAndRoundNumberAndUser_Id(game.getId(), currentRound, currentUserId);
        if (existingAnswer.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You already submitted an answer for this trivia round."
            );
        }

        TriviaRoundAnswerEntity answer = new TriviaRoundAnswerEntity();
        answer.setGame(game);
        answer.setRoundNumber(currentRound);
        answer.setQuestion(currentQuestion);
        answer.setUser(playerOne.getId().equals(currentUserId) ? playerOne : playerTwo);
        answer.setSelectedOptionIndex(selectedOptionIndex);
        answer.setCorrect(selectedOptionIndex == currentQuestion.getCorrectOptionIndex());
        triviaRoundAnswerEntityRepository.save(answer);

        List<TriviaRoundAnswerEntity> currentRoundAnswers = triviaRoundAnswerEntityRepository
                .findAllByGame_IdAndRoundNumberOrderBySubmittedAtAsc(game.getId(), currentRound);

        TriviaRealtimeEventType eventType = TriviaRealtimeEventType.ANSWER_SUBMITTED;
        if (currentRoundAnswers.size() >= REQUIRED_PLAYERS) {
            resolveRound(game, currentRoundAnswers);
            eventType = game.getStatus() == TriviaGameStatus.FINISHED
                    ? TriviaRealtimeEventType.GAME_FINISHED
                    : TriviaRealtimeEventType.ROUND_RESOLVED;
        }

        if (game.getStatus() == TriviaGameStatus.FINISHED && !game.isPlayLimitsApplied()) {
            playLimitService.recordCompletedMatchForUsers(List.of(playerOne.getId(), playerTwo.getId()));
            game.setPlayLimitsApplied(true);
        }

        TriviaGameEntity saved = triviaGameEntityRepository.save(game);
        triviaRealtimePublisher.publishGameEvent(
                eventType,
                roomId,
                saved.getId(),
                currentUserIdText
        );

        return toGameStateResponse(room, members, saved, currentUserId);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveGame(UUID roomId) {
        return triviaGameEntityRepository.existsByRoom_IdAndStatus(roomId, TriviaGameStatus.ACTIVE);
    }

    @Transactional
    public void handleRoomClosed(UUID roomId, String triggeredByUserId) {
        Optional<TriviaGameEntity> existing = triviaGameEntityRepository.findByRoomIdForUpdate(roomId);
        if (existing.isEmpty()) {
            return;
        }

        TriviaGameEntity game = existing.get();
        if (game.getStatus() == TriviaGameStatus.FINISHED) {
            return;
        }

        game.setStatus(TriviaGameStatus.FINISHED);
        game.setCurrentQuestion(null);
        game.setWinnerUser(null);
        game.setDraw(false);
        game.setEndedAt(Instant.now());
        TriviaGameEntity saved = triviaGameEntityRepository.save(game);

        triviaRealtimePublisher.publishGameEvent(
                TriviaRealtimeEventType.GAME_ABORTED,
                roomId,
                saved.getId(),
                triggeredByUserId
        );
    }

    private void resolveRound(TriviaGameEntity game, List<TriviaRoundAnswerEntity> currentRoundAnswers) {
        UserEntity playerOne = requireUser(game.getPlayerOneUser(), "Trivia player one is missing.");
        UserEntity playerTwo = requireUser(game.getPlayerTwoUser(), "Trivia player two is missing.");
        TriviaQuestionEntity currentQuestion = requireQuestion(game.getCurrentQuestion(), "Trivia question is missing.");

        for (TriviaRoundAnswerEntity answer : currentRoundAnswers) {
            if (!answer.isCorrect()) {
                continue;
            }
            if (answer.getUser().getId().equals(playerOne.getId())) {
                game.setPlayerOneScore(game.getPlayerOneScore() + 1);
            } else if (answer.getUser().getId().equals(playerTwo.getId())) {
                game.setPlayerTwoScore(game.getPlayerTwoScore() + 1);
            }
        }

        game.setLastResolvedRound(game.getCurrentRound());
        game.setLastQuestion(currentQuestion);

        if (game.getCurrentRound() >= game.getTotalRounds()) {
            game.setStatus(TriviaGameStatus.FINISHED);
            game.setCurrentQuestion(null);
            game.setEndedAt(Instant.now());

            if (game.getPlayerOneScore() > game.getPlayerTwoScore()) {
                game.setWinnerUser(playerOne);
                game.setDraw(false);
            } else if (game.getPlayerTwoScore() > game.getPlayerOneScore()) {
                game.setWinnerUser(playerTwo);
                game.setDraw(false);
            } else {
                game.setWinnerUser(null);
                game.setDraw(true);
            }

            applyMatchProgress(game);
            return;
        }

        int nextRound = game.getCurrentRound() + 1;
        List<UUID> sequence = parseQuestionSequence(game.getQuestionSequence());
        if (nextRound > sequence.size()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Trivia question sequence is shorter than configured round count."
            );
        }

        TriviaQuestionEntity nextQuestion = triviaQuestionEntityRepository.findById(sequence.get(nextRound - 1))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Trivia question referenced by sequence is missing."
                ));

        game.setCurrentRound(nextRound);
        game.setCurrentQuestion(nextQuestion);
    }

    private void applyMatchProgress(TriviaGameEntity game) {
        UserEntity playerOne = requireUser(game.getPlayerOneUser(), "Trivia player one is missing.");
        UserEntity playerTwo = requireUser(game.getPlayerTwoUser(), "Trivia player two is missing.");

        PlayerProfileEntity playerOneProfile = getRequiredProfile(playerOne.getId());
        PlayerProfileEntity playerTwoProfile = getRequiredProfile(playerTwo.getId());

        playerOneProfile.setGamesPlayed(playerOneProfile.getGamesPlayed() + 1);
        playerTwoProfile.setGamesPlayed(playerTwoProfile.getGamesPlayed() + 1);

        if (game.isDraw()) {
            applyXp(playerOneProfile, XP_DRAW);
            applyXp(playerTwoProfile, XP_DRAW);
        } else if (game.getWinnerUser() != null && game.getWinnerUser().getId().equals(playerOne.getId())) {
            playerOneProfile.setWins(playerOneProfile.getWins() + 1);
            playerTwoProfile.setLosses(playerTwoProfile.getLosses() + 1);
            applyXp(playerOneProfile, XP_WIN);
            applyXp(playerTwoProfile, XP_LOSS);
        } else {
            playerTwoProfile.setWins(playerTwoProfile.getWins() + 1);
            playerOneProfile.setLosses(playerOneProfile.getLosses() + 1);
            applyXp(playerTwoProfile, XP_WIN);
            applyXp(playerOneProfile, XP_LOSS);
        }

        playerProfileEntityRepository.save(playerOneProfile);
        playerProfileEntityRepository.save(playerTwoProfile);
    }

    private static void applyXp(PlayerProfileEntity profile, int xpDelta) {
        int updatedXp = Math.max(0, profile.getXp() + xpDelta);
        profile.setXp(updatedXp);
        int computedLevel = Math.max(1, (updatedXp / XP_PER_LEVEL) + 1);
        profile.setLevel(Math.max(profile.getLevel(), computedLevel));
    }

    private TriviaGameStateResponse toGameStateResponse(
            RoomEntity room,
            List<RoomMemberEntity> members,
            TriviaGameEntity game,
            UUID currentUserId
    ) {
        String status = TriviaGameStatus.WAITING.name();
        int currentRound = 0;
        int totalRounds = room.getRounds();
        TriviaQuestionResponse currentQuestion = null;
        List<String> submittedUserIds = List.of();
        String winnerUserId = null;
        boolean draw = false;
        boolean canStart = room.getHostUser().getId().equals(currentUserId) && members.size() == REQUIRED_PLAYERS;
        boolean canAnswer = false;
        boolean answeredByCurrentUser = false;
        boolean waitingForOpponent = false;
        TriviaRoundOutcomeResponse lastRoundOutcome = null;
        String gameSessionId = null;
        String startedAt = null;
        String endedAt = null;
        String updatedAt = room.getUpdatedAt().toString();

        UUID playerOneId = null;
        UUID playerTwoId = null;
        int playerOneScore = 0;
        int playerTwoScore = 0;

        if (game != null) {
            status = game.getStatus().name();
            currentRound = game.getCurrentRound();
            totalRounds = game.getTotalRounds();
            gameSessionId = game.getId().toString();
            winnerUserId = nullableUserId(game.getWinnerUser());
            draw = game.isDraw();
            startedAt = nullableInstant(game.getStartedAt());
            endedAt = nullableInstant(game.getEndedAt());
            updatedAt = game.getUpdatedAt().toString();
            playerOneId = nullableUserUuid(game.getPlayerOneUser());
            playerTwoId = nullableUserUuid(game.getPlayerTwoUser());
            playerOneScore = game.getPlayerOneScore();
            playerTwoScore = game.getPlayerTwoScore();

            if (game.getStatus() == TriviaGameStatus.ACTIVE && game.getCurrentQuestion() != null) {
                List<TriviaRoundAnswerEntity> currentRoundAnswers = triviaRoundAnswerEntityRepository
                        .findAllByGame_IdAndRoundNumberOrderBySubmittedAtAsc(game.getId(), game.getCurrentRound());
                submittedUserIds = currentRoundAnswers.stream()
                        .map(answer -> answer.getUser().getId().toString())
                        .toList();
                answeredByCurrentUser = submittedUserIds.contains(currentUserId.toString());

                boolean isParticipant = currentUserId.equals(playerOneId) || currentUserId.equals(playerTwoId);
                canAnswer = isParticipant && !answeredByCurrentUser;
                waitingForOpponent = answeredByCurrentUser && currentRoundAnswers.size() < REQUIRED_PLAYERS;

                TriviaQuestionEntity question = game.getCurrentQuestion();
                currentQuestion = new TriviaQuestionResponse(
                        question.getId().toString(),
                        question.getPrompt(),
                        question.getCategory(),
                        question.getDifficulty(),
                        List.of(
                                new TriviaAnswerOptionResponse(0, question.getOptionA()),
                                new TriviaAnswerOptionResponse(1, question.getOptionB()),
                                new TriviaAnswerOptionResponse(2, question.getOptionC()),
                                new TriviaAnswerOptionResponse(3, question.getOptionD())
                        )
                );
            }

            if (game.getLastResolvedRound() != null && game.getLastQuestion() != null) {
                List<TriviaRoundAnswerEntity> lastRoundAnswers = triviaRoundAnswerEntityRepository
                        .findAllByGame_IdAndRoundNumberOrderBySubmittedAtAsc(game.getId(), game.getLastResolvedRound());
                TriviaQuestionEntity lastQuestion = game.getLastQuestion();
                List<TriviaPlayerRoundAnswerResponse> mappedAnswers = lastRoundAnswers.stream()
                        .map(answer -> new TriviaPlayerRoundAnswerResponse(
                                answer.getUser().getId().toString(),
                                answer.getUser().getDisplayName(),
                                answer.getSelectedOptionIndex(),
                                answer.isCorrect()
                        ))
                        .toList();

                String resolvedAt = lastRoundAnswers.stream()
                        .map(TriviaRoundAnswerEntity::getSubmittedAt)
                        .max(Comparator.naturalOrder())
                        .map(Instant::toString)
                        .orElse(updatedAt);

                lastRoundOutcome = new TriviaRoundOutcomeResponse(
                        game.getLastResolvedRound(),
                        lastQuestion.getId().toString(),
                        lastQuestion.getPrompt(),
                        lastQuestion.getCorrectOptionIndex(),
                        mappedAnswers,
                        resolvedAt
                );
            }

            canStart = canStart && (game.getStatus() == TriviaGameStatus.WAITING || game.getStatus() == TriviaGameStatus.FINISHED);
        }

        List<TriviaPlayerScoreResponse> players = resolvePlayers(
                members,
                playerOneId,
                playerTwoId,
                playerOneScore,
                playerTwoScore,
                submittedUserIds
        );

        return new TriviaGameStateResponse(
                room.getId().toString(),
                gameSessionId,
                status,
                currentRound,
                totalRounds,
                currentQuestion,
                players,
                submittedUserIds,
                winnerUserId,
                draw,
                canStart,
                canAnswer,
                answeredByCurrentUser,
                waitingForOpponent,
                lastRoundOutcome,
                startedAt,
                endedAt,
                updatedAt
        );
    }

    private static List<TriviaPlayerScoreResponse> resolvePlayers(
            List<RoomMemberEntity> members,
            UUID playerOneId,
            UUID playerTwoId,
            int playerOneScore,
            int playerTwoScore,
            List<String> submittedUserIds
    ) {
        List<UserEntity> orderedPlayers = new ArrayList<>();
        if (playerOneId != null) {
            findMemberUser(members, playerOneId).ifPresent(orderedPlayers::add);
        }
        if (playerTwoId != null) {
            findMemberUser(members, playerTwoId).ifPresent(orderedPlayers::add);
        }
        for (RoomMemberEntity member : members) {
            if (orderedPlayers.stream().noneMatch(user -> user.getId().equals(member.getUser().getId()))) {
                orderedPlayers.add(member.getUser());
            }
        }

        return orderedPlayers.stream()
                .map(user -> {
                    int score = 0;
                    if (playerOneId != null && playerOneId.equals(user.getId())) {
                        score = playerOneScore;
                    } else if (playerTwoId != null && playerTwoId.equals(user.getId())) {
                        score = playerTwoScore;
                    }
                    return new TriviaPlayerScoreResponse(
                            user.getId().toString(),
                            user.getDisplayName(),
                            score,
                            submittedUserIds.contains(user.getId().toString())
                    );
                })
                .toList();
    }

    private static Optional<UserEntity> findMemberUser(List<RoomMemberEntity> members, UUID userId) {
        return members.stream()
                .map(RoomMemberEntity::getUser)
                .filter(user -> user.getId().equals(userId))
                .findFirst();
    }

    private RoomEntity getRequiredTriviaRoom(UUID roomId) {
        RoomEntity room = roomEntityRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found: " + roomId));
        if (!TRIVIA_CODE.equalsIgnoreCase(room.getGameType().getCode())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This room is not configured for Trivia Battle.");
        }
        return room;
    }

    private static void assertHostOnly(RoomEntity room, UUID userId) {
        if (!room.getHostUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room host can start Trivia Battle.");
        }
    }

    private static void assertUserIsRoomMember(List<RoomMemberEntity> members, UUID userId) {
        boolean isMember = members.stream().anyMatch(member -> member.getUser().getId().equals(userId));
        if (!isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only room members can access this game.");
        }
    }

    private PlayerProfileEntity getRequiredProfile(UUID userId) {
        return playerProfileEntityRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player profile is missing for user: " + userId
                ));
    }

    private static UserEntity requireUser(UserEntity user, String message) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
        return user;
    }

    private static TriviaQuestionEntity requireQuestion(TriviaQuestionEntity question, String message) {
        if (question == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, message);
        }
        return question;
    }

    private static String encodeQuestionSequence(List<UUID> questionIds) {
        return questionIds.stream().map(UUID::toString).reduce((left, right) -> left + "," + right).orElse("");
    }

    private static List<UUID> parseQuestionSequence(String questionSequence) {
        if (questionSequence == null || questionSequence.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Trivia question sequence is unavailable."
            );
        }

        String[] parts = questionSequence.split(",");
        List<UUID> parsed = new ArrayList<>(parts.length);
        for (String part : parts) {
            try {
                parsed.add(UUID.fromString(part.trim()));
            } catch (IllegalArgumentException ex) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Trivia question sequence contains an invalid question identifier."
                );
            }
        }
        return parsed;
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

    private static UUID nullableUserUuid(UserEntity user) {
        return user == null ? null : user.getId();
    }

    private static String nullableUserId(UserEntity user) {
        return user == null ? null : user.getId().toString();
    }

    private static String nullableInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }
}
