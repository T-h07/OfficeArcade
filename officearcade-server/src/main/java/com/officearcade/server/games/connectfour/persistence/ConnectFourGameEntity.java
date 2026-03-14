package com.officearcade.server.games.connectfour.persistence;

import com.officearcade.server.games.connectfour.ConnectFourGameStatus;
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
@Table(name = "connect_four_games")
public class ConnectFourGameEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false, unique = true)
    private RoomEntity room;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ConnectFourGameStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_one_user_id")
    private UserEntity playerOneUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_two_user_id")
    private UserEntity playerTwoUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_turn_user_id")
    private UserEntity currentTurnUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_user_id")
    private UserEntity winnerUser;

    @Column(name = "board_state", nullable = false, length = 42)
    private String boardState;

    @Column(name = "move_count", nullable = false)
    private int moveCount;

    @Column(name = "is_draw", nullable = false)
    private boolean draw;

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

    public ConnectFourGameStatus getStatus() {
        return status;
    }

    public void setStatus(ConnectFourGameStatus status) {
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

    public UserEntity getCurrentTurnUser() {
        return currentTurnUser;
    }

    public void setCurrentTurnUser(UserEntity currentTurnUser) {
        this.currentTurnUser = currentTurnUser;
    }

    public UserEntity getWinnerUser() {
        return winnerUser;
    }

    public void setWinnerUser(UserEntity winnerUser) {
        this.winnerUser = winnerUser;
    }

    public String getBoardState() {
        return boardState;
    }

    public void setBoardState(String boardState) {
        this.boardState = boardState;
    }

    public int getMoveCount() {
        return moveCount;
    }

    public void setMoveCount(int moveCount) {
        this.moveCount = moveCount;
    }

    public boolean isDraw() {
        return draw;
    }

    public void setDraw(boolean draw) {
        this.draw = draw;
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
