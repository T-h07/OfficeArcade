package com.officearcade.server.games.uno.persistence;

import com.officearcade.server.games.uno.UnoGameStatus;
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
@Table(name = "uno_games")
public class UnoGameEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private RoomEntity room;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private UnoGameStatus status;

    @Column(name = "player_order_state", nullable = false, columnDefinition = "TEXT")
    private String playerOrderState;

    @Column(name = "current_turn_index", nullable = false)
    private int currentTurnIndex;

    @Column(name = "direction", nullable = false)
    private int direction;

    @Column(name = "current_color", length = 16)
    private String currentColor;

    @Column(name = "draw_pile_state", nullable = false, columnDefinition = "TEXT")
    private String drawPileState;

    @Column(name = "discard_pile_state", nullable = false, columnDefinition = "TEXT")
    private String discardPileState;

    @Column(name = "hands_state", nullable = false, columnDefinition = "TEXT")
    private String handsState;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_user_id")
    private UserEntity winnerUser;

    @Column(name = "move_count", nullable = false)
    private int moveCount;

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

    public UnoGameStatus getStatus() {
        return status;
    }

    public void setStatus(UnoGameStatus status) {
        this.status = status;
    }

    public String getPlayerOrderState() {
        return playerOrderState;
    }

    public void setPlayerOrderState(String playerOrderState) {
        this.playerOrderState = playerOrderState;
    }

    public int getCurrentTurnIndex() {
        return currentTurnIndex;
    }

    public void setCurrentTurnIndex(int currentTurnIndex) {
        this.currentTurnIndex = currentTurnIndex;
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public String getCurrentColor() {
        return currentColor;
    }

    public void setCurrentColor(String currentColor) {
        this.currentColor = currentColor;
    }

    public String getDrawPileState() {
        return drawPileState;
    }

    public void setDrawPileState(String drawPileState) {
        this.drawPileState = drawPileState;
    }

    public String getDiscardPileState() {
        return discardPileState;
    }

    public void setDiscardPileState(String discardPileState) {
        this.discardPileState = discardPileState;
    }

    public String getHandsState() {
        return handsState;
    }

    public void setHandsState(String handsState) {
        this.handsState = handsState;
    }

    public UserEntity getWinnerUser() {
        return winnerUser;
    }

    public void setWinnerUser(UserEntity winnerUser) {
        this.winnerUser = winnerUser;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
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
