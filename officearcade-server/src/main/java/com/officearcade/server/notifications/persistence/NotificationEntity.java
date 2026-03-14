package com.officearcade.server.notifications.persistence;

import com.officearcade.server.notifications.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class NotificationEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 64)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 140)
    private String title;

    @Column(name = "message", nullable = false, length = 320)
    private String message;

    @Column(name = "navigation_path", length = 200)
    private String navigationPath;

    @Column(name = "source_room_id")
    private UUID sourceRoomId;

    @Column(name = "source_game_session_id")
    private UUID sourceGameSessionId;

    @Column(name = "source_challenge_id")
    private UUID sourceChallengeId;

    @Column(name = "source_store_item_id")
    private UUID sourceStoreItemId;

    @Column(name = "source_report_id")
    private UUID sourceReportId;

    @Column(name = "event_key", length = 160)
    private String eventKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNavigationPath() {
        return navigationPath;
    }

    public void setNavigationPath(String navigationPath) {
        this.navigationPath = navigationPath;
    }

    public UUID getSourceRoomId() {
        return sourceRoomId;
    }

    public void setSourceRoomId(UUID sourceRoomId) {
        this.sourceRoomId = sourceRoomId;
    }

    public UUID getSourceGameSessionId() {
        return sourceGameSessionId;
    }

    public void setSourceGameSessionId(UUID sourceGameSessionId) {
        this.sourceGameSessionId = sourceGameSessionId;
    }

    public UUID getSourceChallengeId() {
        return sourceChallengeId;
    }

    public void setSourceChallengeId(UUID sourceChallengeId) {
        this.sourceChallengeId = sourceChallengeId;
    }

    public UUID getSourceStoreItemId() {
        return sourceStoreItemId;
    }

    public void setSourceStoreItemId(UUID sourceStoreItemId) {
        this.sourceStoreItemId = sourceStoreItemId;
    }

    public UUID getSourceReportId() {
        return sourceReportId;
    }

    public void setSourceReportId(UUID sourceReportId) {
        this.sourceReportId = sourceReportId;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
