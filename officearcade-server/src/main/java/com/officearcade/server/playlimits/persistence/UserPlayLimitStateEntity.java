package com.officearcade.server.playlimits.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "player_play_limits")
public class UserPlayLimitStateEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "games_played_date", nullable = false)
    private LocalDate gamesPlayedDate;

    @Column(name = "games_played_today", nullable = false)
    private int gamesPlayedToday;

    @Column(name = "cooldown_until")
    private Instant cooldownUntil;

    @Column(name = "last_completed_game_at")
    private Instant lastCompletedGameAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public LocalDate getGamesPlayedDate() {
        return gamesPlayedDate;
    }

    public void setGamesPlayedDate(LocalDate gamesPlayedDate) {
        this.gamesPlayedDate = gamesPlayedDate;
    }

    public int getGamesPlayedToday() {
        return gamesPlayedToday;
    }

    public void setGamesPlayedToday(int gamesPlayedToday) {
        this.gamesPlayedToday = gamesPlayedToday;
    }

    public Instant getCooldownUntil() {
        return cooldownUntil;
    }

    public void setCooldownUntil(Instant cooldownUntil) {
        this.cooldownUntil = cooldownUntil;
    }

    public Instant getLastCompletedGameAt() {
        return lastCompletedGameAt;
    }

    public void setLastCompletedGameAt(Instant lastCompletedGameAt) {
        this.lastCompletedGameAt = lastCompletedGameAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
