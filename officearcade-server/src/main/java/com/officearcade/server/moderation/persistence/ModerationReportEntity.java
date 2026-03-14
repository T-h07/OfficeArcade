package com.officearcade.server.moderation.persistence;

import com.officearcade.server.challenges.persistence.PostMatchChallengeEntity;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntity;
import com.officearcade.server.lobby.persistence.RoomEntity;
import com.officearcade.server.moderation.ModerationReportCategory;
import com.officearcade.server.moderation.ModerationReportStatus;
import com.officearcade.server.users.persistence.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "moderation_reports")
public class ModerationReportEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private UserEntity reporterUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private UserEntity reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64)
    private ModerationReportCategory category;

    @Column(name = "note", length = 280)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ModerationReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_room_id")
    private RoomEntity sourceRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_game_session_id")
    private ConnectFourGameEntity sourceGameSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_challenge_id")
    private PostMatchChallengeEntity sourceChallenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_id")
    private UserEntity reviewedByAdmin;

    @Column(name = "resolution_note", length = 280)
    private String resolutionNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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

    public UserEntity getReporterUser() {
        return reporterUser;
    }

    public void setReporterUser(UserEntity reporterUser) {
        this.reporterUser = reporterUser;
    }

    public UserEntity getReportedUser() {
        return reportedUser;
    }

    public void setReportedUser(UserEntity reportedUser) {
        this.reportedUser = reportedUser;
    }

    public ModerationReportCategory getCategory() {
        return category;
    }

    public void setCategory(ModerationReportCategory category) {
        this.category = category;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ModerationReportStatus getStatus() {
        return status;
    }

    public void setStatus(ModerationReportStatus status) {
        this.status = status;
    }

    public RoomEntity getSourceRoom() {
        return sourceRoom;
    }

    public void setSourceRoom(RoomEntity sourceRoom) {
        this.sourceRoom = sourceRoom;
    }

    public ConnectFourGameEntity getSourceGameSession() {
        return sourceGameSession;
    }

    public void setSourceGameSession(ConnectFourGameEntity sourceGameSession) {
        this.sourceGameSession = sourceGameSession;
    }

    public PostMatchChallengeEntity getSourceChallenge() {
        return sourceChallenge;
    }

    public void setSourceChallenge(PostMatchChallengeEntity sourceChallenge) {
        this.sourceChallenge = sourceChallenge;
    }

    public UserEntity getReviewedByAdmin() {
        return reviewedByAdmin;
    }

    public void setReviewedByAdmin(UserEntity reviewedByAdmin) {
        this.reviewedByAdmin = reviewedByAdmin;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
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
