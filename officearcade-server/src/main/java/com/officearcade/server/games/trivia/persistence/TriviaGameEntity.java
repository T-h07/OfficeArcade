package com.officearcade.server.games.trivia.persistence;

import com.officearcade.server.games.trivia.TriviaGameStatus;
import com.officearcade.server.lobby.persistence.RoomEntity;
import com.officearcade.server.users.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "trivia_games")
public class TriviaGameEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private RoomEntity room;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TriviaGameStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_one_user_id")
    private UserEntity playerOneUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_two_user_id")
    private UserEntity playerTwoUser;

    @Column(name = "current_round", nullable = false)
    private int currentRound;

    @Column(name = "total_rounds", nullable = false)
    private int totalRounds;

    @Column(name = "question_sequence", nullable = false, length = 512)
    private String questionSequence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_question_id")
    private TriviaQuestionEntity currentQuestion;

    @Column(name = "last_resolved_round")
    private Integer lastResolvedRound;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_question_id")
    private TriviaQuestionEntity lastQuestion;

    @Column(name = "player_one_score", nullable = false)
    private int playerOneScore;

    @Column(name = "player_two_score", nullable = false)
    private int playerTwoScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_user_id")
    private UserEntity winnerUser;

    @Column(name = "is_draw", nullable = false)
    private boolean draw;

    @Column(name = "play_limits_applied", nullable = false)
    private boolean playLimitsApplied;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public void setRoom(RoomEntity room) {
        this.room = room;
    }

    public TriviaGameStatus getStatus() {
        return status;
    }

    public void setStatus(TriviaGameStatus status) {
        this.status = status;
    }

    public UserEntity getPlayerOneUser() {
        return playerOneUser;
    }

    public void setPlayerOneUser(UserEntity playerOneUser) {
        this.playerOneUser = playerOneUser;
    }

    public UserEntity getPlayerTwoUser() {
        return playerTwoUser;
    }

    public void setPlayerTwoUser(UserEntity playerTwoUser) {
        this.playerTwoUser = playerTwoUser;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public void setTotalRounds(int totalRounds) {
        this.totalRounds = totalRounds;
    }

    public String getQuestionSequence() {
        return questionSequence;
    }

    public void setQuestionSequence(String questionSequence) {
        this.questionSequence = questionSequence;
    }

    public TriviaQuestionEntity getCurrentQuestion() {
        return currentQuestion;
    }

    public void setCurrentQuestion(TriviaQuestionEntity currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public Integer getLastResolvedRound() {
        return lastResolvedRound;
    }

    public void setLastResolvedRound(Integer lastResolvedRound) {
        this.lastResolvedRound = lastResolvedRound;
    }

    public TriviaQuestionEntity getLastQuestion() {
        return lastQuestion;
    }

    public void setLastQuestion(TriviaQuestionEntity lastQuestion) {
        this.lastQuestion = lastQuestion;
    }

    public int getPlayerOneScore() {
        return playerOneScore;
    }

    public void setPlayerOneScore(int playerOneScore) {
        this.playerOneScore = playerOneScore;
    }

    public int getPlayerTwoScore() {
        return playerTwoScore;
    }

    public void setPlayerTwoScore(int playerTwoScore) {
        this.playerTwoScore = playerTwoScore;
    }

    public UserEntity getWinnerUser() {
        return winnerUser;
    }

    public void setWinnerUser(UserEntity winnerUser) {
        this.winnerUser = winnerUser;
    }

    public boolean isDraw() {
        return draw;
    }

    public void setDraw(boolean draw) {
        this.draw = draw;
    }

    public boolean isPlayLimitsApplied() {
        return playLimitsApplied;
    }

    public void setPlayLimitsApplied(boolean playLimitsApplied) {
        this.playLimitsApplied = playLimitsApplied;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
