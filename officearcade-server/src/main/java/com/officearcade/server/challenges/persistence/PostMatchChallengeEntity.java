package com.officearcade.server.challenges.persistence;

import com.officearcade.server.challenges.ChallengeStatus;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntity;
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
@Table(name = "post_match_challenges")
public class PostMatchChallengeEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_game_session_id", nullable = false, unique = true)
    private ConnectFourGameEntity sourceGameSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_room_id", nullable = false)
    private RoomEntity sourceRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "challenge_type_id", nullable = false)
    private ChallengeTypeEntity challengeType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "obligated_user_id", nullable = false)
    private UserEntity obligatedUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_user_id", nullable = false)
    private UserEntity beneficiaryUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ChallengeStatus status;

    @Column(name = "respect_points_awarded", nullable = false)
    private int respectPointsAwarded;

    @Column(name = "karma_points_awarded", nullable = false)
    private int karmaPointsAwarded;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "disputed_at")
    private Instant disputedAt;

    @Column(name = "dispute_note", length = 280)
    private String disputeNote;

    @Column(name = "resolution_note", length = 280)
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_admin_id")
    private UserEntity resolvedByAdmin;

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

    public ConnectFourGameEntity getSourceGameSession() {
        return sourceGameSession;
    }

    public void setSourceGameSession(ConnectFourGameEntity sourceGameSession) {
        this.sourceGameSession = sourceGameSession;
    }

    public RoomEntity getSourceRoom() {
        return sourceRoom;
    }

    public void setSourceRoom(RoomEntity sourceRoom) {
        this.sourceRoom = sourceRoom;
    }

    public ChallengeTypeEntity getChallengeType() {
        return challengeType;
    }

    public void setChallengeType(ChallengeTypeEntity challengeType) {
        this.challengeType = challengeType;
    }

    public UserEntity getObligatedUser() {
        return obligatedUser;
    }

    public void setObligatedUser(UserEntity obligatedUser) {
        this.obligatedUser = obligatedUser;
    }

    public UserEntity getBeneficiaryUser() {
        return beneficiaryUser;
    }

    public void setBeneficiaryUser(UserEntity beneficiaryUser) {
        this.beneficiaryUser = beneficiaryUser;
    }

    public ChallengeStatus getStatus() {
        return status;
    }

    public void setStatus(ChallengeStatus status) {
        this.status = status;
    }

    public int getRespectPointsAwarded() {
        return respectPointsAwarded;
    }

    public void setRespectPointsAwarded(int respectPointsAwarded) {
        this.respectPointsAwarded = respectPointsAwarded;
    }

    public int getKarmaPointsAwarded() {
        return karmaPointsAwarded;
    }

    public void setKarmaPointsAwarded(int karmaPointsAwarded) {
        this.karmaPointsAwarded = karmaPointsAwarded;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public Instant getDisputedAt() {
        return disputedAt;
    }

    public void setDisputedAt(Instant disputedAt) {
        this.disputedAt = disputedAt;
    }

    public String getDisputeNote() {
        return disputeNote;
    }

    public void setDisputeNote(String disputeNote) {
        this.disputeNote = disputeNote;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public UserEntity getResolvedByAdmin() {
        return resolvedByAdmin;
    }

    public void setResolvedByAdmin(UserEntity resolvedByAdmin) {
        this.resolvedByAdmin = resolvedByAdmin;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
