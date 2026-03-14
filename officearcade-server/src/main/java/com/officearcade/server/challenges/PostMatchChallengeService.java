package com.officearcade.server.challenges;

import com.officearcade.server.challenges.dto.ChallengeListResponse;
import com.officearcade.server.challenges.dto.ChallengeSummaryResponse;
import com.officearcade.server.challenges.dto.DashboardChallengeSummaryResponse;
import com.officearcade.server.challenges.persistence.ChallengeTypeEntity;
import com.officearcade.server.challenges.persistence.ChallengeTypeEntityRepository;
import com.officearcade.server.challenges.persistence.PostMatchChallengeEntity;
import com.officearcade.server.challenges.persistence.PostMatchChallengeEntityRepository;
import com.officearcade.server.games.connectfour.ConnectFourGameStatus;
import com.officearcade.server.games.connectfour.dto.ConnectFourChallengeSummaryResponse;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntity;
import com.officearcade.server.notifications.NotificationCreateCommand;
import com.officearcade.server.notifications.NotificationService;
import com.officearcade.server.notifications.NotificationType;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
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
public class PostMatchChallengeService {

    private final ChallengeTypeEntityRepository challengeTypeEntityRepository;
    private final PostMatchChallengeEntityRepository postMatchChallengeEntityRepository;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final NotificationService notificationService;

    public PostMatchChallengeService(
            ChallengeTypeEntityRepository challengeTypeEntityRepository,
            PostMatchChallengeEntityRepository postMatchChallengeEntityRepository,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            NotificationService notificationService
    ) {
        this.challengeTypeEntityRepository = challengeTypeEntityRepository;
        this.postMatchChallengeEntityRepository = postMatchChallengeEntityRepository;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Optional<PostMatchChallengeEntity> createForCompletedConnectFour(ConnectFourGameEntity gameSession) {
        if (gameSession == null || gameSession.getId() == null) {
            return Optional.empty();
        }
        if (gameSession.getStatus() != ConnectFourGameStatus.FINISHED || gameSession.isDraw()) {
            return Optional.empty();
        }
        if (gameSession.getWinnerUser() == null || gameSession.getPlayerOneUser() == null || gameSession.getPlayerTwoUser() == null) {
            return Optional.empty();
        }

        Optional<PostMatchChallengeEntity> existing = postMatchChallengeEntityRepository
                .findBySourceGameSession_Id(gameSession.getId());
        if (existing.isPresent()) {
            return existing;
        }

        UserEntity winner = gameSession.getWinnerUser();
        UserEntity loser;
        if (winner.getId().equals(gameSession.getPlayerOneUser().getId())) {
            loser = gameSession.getPlayerTwoUser();
        } else if (winner.getId().equals(gameSession.getPlayerTwoUser().getId())) {
            loser = gameSession.getPlayerOneUser();
        } else {
            return Optional.empty();
        }

        ChallengeTypeEntity challengeType = selectSafeChallengeType(gameSession.getId());

        PostMatchChallengeEntity challenge = new PostMatchChallengeEntity();
        challenge.setSourceGameSession(gameSession);
        challenge.setSourceRoom(gameSession.getRoom());
        challenge.setChallengeType(challengeType);
        challenge.setObligatedUser(loser);
        challenge.setBeneficiaryUser(winner);
        challenge.setStatus(ChallengeStatus.PENDING);
        challenge.setRespectPointsAwarded(0);
        challenge.setKarmaPointsAwarded(0);
        challenge.setDisputedAt(null);
        challenge.setDisputeNote(null);
        challenge.setResolutionNote(null);
        challenge.setResolvedByAdmin(null);
        challenge.setResolvedAt(null);

        PostMatchChallengeEntity saved = postMatchChallengeEntityRepository.save(challenge);
        notifyChallengeCreated(saved);
        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public Optional<ConnectFourChallengeSummaryResponse> findConnectFourChallengeSummary(UUID sourceGameSessionId) {
        return postMatchChallengeEntityRepository.findBySourceGameSession_Id(sourceGameSessionId)
                .map(this::toConnectFourSummary);
    }

    @Transactional(readOnly = true)
    public ChallengeListResponse listChallengesForUser(String currentUserIdText) {
        UUID currentUserId = parseUserId(currentUserIdText);
        List<PostMatchChallengeEntity> challenges = postMatchChallengeEntityRepository.findAllForUser(currentUserId);

        List<ChallengeSummaryResponse> mapped = challenges.stream()
                .map(challenge -> toChallengeSummary(challenge, currentUserId))
                .toList();

        int pendingCount = (int) (
                postMatchChallengeEntityRepository.countByStatusForUser(ChallengeStatus.PENDING, currentUserId)
                        + postMatchChallengeEntityRepository.countByStatusForUser(ChallengeStatus.DISPUTED, currentUserId)
        );
        int resolvedCount = mapped.size() - pendingCount;

        return new ChallengeListResponse(mapped.size(), pendingCount, Math.max(0, resolvedCount), mapped);
    }

    @Transactional(readOnly = true)
    public ChallengeSummaryResponse getChallengeForUser(String currentUserIdText, String challengeIdText) {
        UUID currentUserId = parseUserId(currentUserIdText);
        UUID challengeId = parseChallengeId(challengeIdText);

        PostMatchChallengeEntity challenge = postMatchChallengeEntityRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found: " + challengeId));

        assertUserIsChallengeParticipant(challenge, currentUserId);
        return toChallengeSummary(challenge, currentUserId);
    }

    @Transactional
    public ChallengeSummaryResponse confirmChallenge(String currentUserIdText, String challengeIdText) {
        return resolveChallenge(currentUserIdText, challengeIdText, ChallengeStatus.COMPLETED_CONFIRMED);
    }

    @Transactional
    public ChallengeSummaryResponse rejectChallenge(String currentUserIdText, String challengeIdText) {
        return resolveChallenge(currentUserIdText, challengeIdText, ChallengeStatus.REJECTED);
    }

    @Transactional
    public ChallengeSummaryResponse disputeChallenge(String currentUserIdText, String challengeIdText, String note) {
        UUID currentUserId = parseUserId(currentUserIdText);
        UUID challengeId = parseChallengeId(challengeIdText);

        PostMatchChallengeEntity challenge = postMatchChallengeEntityRepository.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found: " + challengeId));

        assertUserIsChallengeParticipant(challenge, currentUserId);
        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only pending challenges can be disputed.");
        }

        challenge.setStatus(ChallengeStatus.DISPUTED);
        challenge.setDisputedAt(Instant.now());
        challenge.setDisputeNote(normalizeOptionalNote(note));
        challenge.setResolutionNote(null);
        challenge.setResolvedByAdmin(null);
        challenge.setResolvedAt(null);

        PostMatchChallengeEntity saved = postMatchChallengeEntityRepository.save(challenge);
        return toChallengeSummary(saved, currentUserId);
    }

    @Transactional(readOnly = true)
    public int getPendingChallengeCount(String currentUserIdText) {
        UUID userId = parseUserId(currentUserIdText);
        long pending = postMatchChallengeEntityRepository.countByStatusForUser(ChallengeStatus.PENDING, userId);
        long disputed = postMatchChallengeEntityRepository.countByStatusForUser(ChallengeStatus.DISPUTED, userId);
        return (int) (pending + disputed);
    }

    @Transactional(readOnly = true)
    public int getResolvedChallengeCount(String currentUserIdText) {
        UUID userId = parseUserId(currentUserIdText);
        long confirmed = postMatchChallengeEntityRepository.countByStatusForUser(ChallengeStatus.COMPLETED_CONFIRMED, userId);
        long rejected = postMatchChallengeEntityRepository.countByStatusForUser(ChallengeStatus.REJECTED, userId);
        long cancelled = postMatchChallengeEntityRepository.countByStatusForUser(ChallengeStatus.CANCELLED, userId);
        long expired = postMatchChallengeEntityRepository.countByStatusForUser(ChallengeStatus.EXPIRED, userId);
        return (int) (confirmed + rejected + cancelled + expired);
    }

    @Transactional(readOnly = true)
    public List<DashboardChallengeSummaryResponse> getRecentChallengeSummaries(String currentUserIdText, int limit) {
        UUID userId = parseUserId(currentUserIdText);
        int safeLimit = Math.max(0, limit);
        if (safeLimit == 0) {
            return List.of();
        }

        return postMatchChallengeEntityRepository.findAllForUser(userId).stream()
                .limit(safeLimit)
                .map(challenge -> toDashboardSummary(challenge, userId))
                .toList();
    }

    private ChallengeSummaryResponse resolveChallenge(
            String currentUserIdText,
            String challengeIdText,
            ChallengeStatus resolutionStatus
    ) {
        UUID currentUserId = parseUserId(currentUserIdText);
        UUID challengeId = parseChallengeId(challengeIdText);

        if (resolutionStatus != ChallengeStatus.COMPLETED_CONFIRMED && resolutionStatus != ChallengeStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported challenge resolution state.");
        }

        PostMatchChallengeEntity challenge = postMatchChallengeEntityRepository.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found: " + challengeId));

        assertUserIsChallengeParticipant(challenge, currentUserId);
        assertUserIsBeneficiary(challenge, currentUserId);

        if (challenge.getStatus() == ChallengeStatus.DISPUTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Challenge is under moderation review.");
        }
        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Challenge is already resolved.");
        }

        PlayerProfileEntity obligatedProfile = getRequiredProfile(challenge.getObligatedUser().getId());
        ChallengeTypeEntity challengeType = challenge.getChallengeType();

        if (resolutionStatus == ChallengeStatus.COMPLETED_CONFIRMED) {
            int respectAward = Math.max(challengeType.getRespectRewardPoints(), 0);
            obligatedProfile.setRespectPoints(obligatedProfile.getRespectPoints() + respectAward);
            challenge.setRespectPointsAwarded(respectAward);
            challenge.setKarmaPointsAwarded(0);
        } else {
            int karmaAward = Math.max(challengeType.getKarmaPenaltyPoints(), 0);
            obligatedProfile.setKarmaPoints(obligatedProfile.getKarmaPoints() + karmaAward);
            challenge.setRespectPointsAwarded(0);
            challenge.setKarmaPointsAwarded(karmaAward);
        }

        challenge.setStatus(resolutionStatus);
        challenge.setResolvedAt(Instant.now());
        challenge.setResolvedByAdmin(null);
        challenge.setResolutionNote(null);
        playerProfileEntityRepository.save(obligatedProfile);
        PostMatchChallengeEntity savedChallenge = postMatchChallengeEntityRepository.save(challenge);
        notifyChallengeResolved(savedChallenge, resolutionStatus);

        return toChallengeSummary(savedChallenge, currentUserId);
    }

    private ChallengeSummaryResponse toChallengeSummary(PostMatchChallengeEntity challenge, UUID currentUserId) {
        ChallengeParticipantRole myRole = resolveParticipantRole(challenge, currentUserId);
        return new ChallengeSummaryResponse(
                challenge.getId().toString(),
                challenge.getSourceGameSession().getId().toString(),
                challenge.getSourceRoom().getId().toString(),
                challenge.getChallengeType().getCode(),
                challenge.getChallengeType().getDisplayName(),
                challenge.getChallengeType().getDescription(),
                challenge.getStatus().name(),
                challenge.getObligatedUser().getId().toString(),
                challenge.getObligatedUser().getDisplayName(),
                challenge.getBeneficiaryUser().getId().toString(),
                challenge.getBeneficiaryUser().getDisplayName(),
                myRole.name(),
                myRole == ChallengeParticipantRole.BENEFICIARY && challenge.getStatus() == ChallengeStatus.PENDING,
                challenge.getRespectPointsAwarded(),
                challenge.getKarmaPointsAwarded(),
                challenge.getCreatedAt().toString(),
                nullableInstant(challenge.getResolvedAt()),
                nullableInstant(challenge.getDisputedAt()),
                challenge.getDisputeNote(),
                challenge.getResolutionNote(),
                nullableUserId(challenge.getResolvedByAdmin())
        );
    }

    private DashboardChallengeSummaryResponse toDashboardSummary(PostMatchChallengeEntity challenge, UUID currentUserId) {
        ChallengeParticipantRole myRole = resolveParticipantRole(challenge, currentUserId);
        String counterpartyDisplayName = myRole == ChallengeParticipantRole.BENEFICIARY
                ? challenge.getObligatedUser().getDisplayName()
                : challenge.getBeneficiaryUser().getDisplayName();

        return new DashboardChallengeSummaryResponse(
                challenge.getId().toString(),
                challenge.getChallengeType().getCode(),
                challenge.getChallengeType().getDisplayName(),
                challenge.getStatus().name(),
                myRole.name(),
                counterpartyDisplayName,
                challenge.getCreatedAt().toString(),
                nullableInstant(challenge.getResolvedAt())
        );
    }

    private ConnectFourChallengeSummaryResponse toConnectFourSummary(PostMatchChallengeEntity challenge) {
        return new ConnectFourChallengeSummaryResponse(
                challenge.getId().toString(),
                challenge.getChallengeType().getCode(),
                challenge.getChallengeType().getDisplayName(),
                challenge.getStatus().name(),
                challenge.getObligatedUser().getId().toString(),
                challenge.getBeneficiaryUser().getId().toString(),
                challenge.getRespectPointsAwarded(),
                challenge.getKarmaPointsAwarded(),
                challenge.getCreatedAt().toString(),
                nullableInstant(challenge.getResolvedAt())
        );
    }

    private static void assertUserIsChallengeParticipant(PostMatchChallengeEntity challenge, UUID currentUserId) {
        boolean isParticipant = challenge.getObligatedUser().getId().equals(currentUserId)
                || challenge.getBeneficiaryUser().getId().equals(currentUserId);
        if (!isParticipant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this challenge.");
        }
    }

    private static void assertUserIsBeneficiary(PostMatchChallengeEntity challenge, UUID currentUserId) {
        if (!challenge.getBeneficiaryUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the beneficiary can confirm or reject this challenge."
            );
        }
    }

    private static ChallengeParticipantRole resolveParticipantRole(PostMatchChallengeEntity challenge, UUID currentUserId) {
        if (challenge.getObligatedUser().getId().equals(currentUserId)) {
            return ChallengeParticipantRole.OBLIGATED;
        }
        if (challenge.getBeneficiaryUser().getId().equals(currentUserId)) {
            return ChallengeParticipantRole.BENEFICIARY;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have access to this challenge.");
    }

    private PlayerProfileEntity getRequiredProfile(UUID userId) {
        return playerProfileEntityRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player profile is missing for user: " + userId
                ));
    }

    private ChallengeTypeEntity selectSafeChallengeType(UUID gameSessionId) {
        List<ChallengeTypeEntity> enabledChallengeTypes = challengeTypeEntityRepository.findAllByEnabledTrueOrderByDisplayNameAsc();
        if (enabledChallengeTypes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No enabled safe challenge types are configured."
            );
        }

        int index = Math.floorMod(gameSessionId.hashCode(), enabledChallengeTypes.size());
        return enabledChallengeTypes.get(index);
    }

    private static UUID parseUserId(String userIdText) {
        try {
            return UUID.fromString(userIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        }
    }

    private static UUID parseChallengeId(String challengeIdText) {
        try {
            return UUID.fromString(challengeIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Challenge id must be a valid UUID.");
        }
    }

    private static String nullableInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static String nullableUserId(UserEntity user) {
        return user == null ? null : user.getId().toString();
    }

    private static String normalizeOptionalNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > 280) {
            return trimmed.substring(0, 280);
        }
        return trimmed;
    }

    private void notifyChallengeCreated(PostMatchChallengeEntity challenge) {
        String navigationPath = "/app/challenges";
        String eventKey = "challenge-created:" + challenge.getId();

        NotificationCreateCommand obligatedNotification = new NotificationCreateCommand(
                challenge.getObligatedUser().getId(),
                NotificationType.CHALLENGE_CREATED,
                "New challenge created",
                "Challenge created from your recent match against " + challenge.getBeneficiaryUser().getDisplayName() + ".",
                navigationPath,
                challenge.getSourceRoom().getId(),
                challenge.getSourceGameSession().getId(),
                challenge.getId(),
                null,
                null,
                eventKey
        );

        NotificationCreateCommand beneficiaryNotification = new NotificationCreateCommand(
                challenge.getBeneficiaryUser().getId(),
                NotificationType.CHALLENGE_CREATED,
                "Challenge ready for review",
                "A post-match challenge against " + challenge.getObligatedUser().getDisplayName() + " is pending your decision.",
                navigationPath,
                challenge.getSourceRoom().getId(),
                challenge.getSourceGameSession().getId(),
                challenge.getId(),
                null,
                null,
                eventKey
        );

        notificationService.safeCreateNotifications(List.of(obligatedNotification, beneficiaryNotification));
    }

    private void notifyChallengeResolved(PostMatchChallengeEntity challenge, ChallengeStatus resolutionStatus) {
        if (resolutionStatus != ChallengeStatus.COMPLETED_CONFIRMED && resolutionStatus != ChallengeStatus.REJECTED) {
            return;
        }

        String navigationPath = "/app/challenges";
        if (resolutionStatus == ChallengeStatus.COMPLETED_CONFIRMED) {
            NotificationCreateCommand obligatedChallenge = new NotificationCreateCommand(
                    challenge.getObligatedUser().getId(),
                    NotificationType.CHALLENGE_CONFIRMED,
                    "Challenge confirmed",
                    "Your challenge was confirmed by " + challenge.getBeneficiaryUser().getDisplayName() + ".",
                    navigationPath,
                    challenge.getSourceRoom().getId(),
                    challenge.getSourceGameSession().getId(),
                    challenge.getId(),
                    null,
                    null,
                    "challenge-confirmed:" + challenge.getId()
            );
            NotificationCreateCommand obligatedRespect = new NotificationCreateCommand(
                    challenge.getObligatedUser().getId(),
                    NotificationType.RESPECT_GAINED,
                    "Respect awarded",
                    "You gained " + challenge.getRespectPointsAwarded() + " Respect from challenge resolution.",
                    navigationPath,
                    challenge.getSourceRoom().getId(),
                    challenge.getSourceGameSession().getId(),
                    challenge.getId(),
                    null,
                    null,
                    "respect-gained:" + challenge.getId()
            );
            NotificationCreateCommand beneficiaryChallenge = new NotificationCreateCommand(
                    challenge.getBeneficiaryUser().getId(),
                    NotificationType.CHALLENGE_CONFIRMED,
                    "Challenge resolved",
                    "You confirmed a challenge outcome for " + challenge.getObligatedUser().getDisplayName() + ".",
                    navigationPath,
                    challenge.getSourceRoom().getId(),
                    challenge.getSourceGameSession().getId(),
                    challenge.getId(),
                    null,
                    null,
                    "challenge-confirmed:" + challenge.getId()
            );
            notificationService.safeCreateNotifications(List.of(
                    obligatedChallenge,
                    obligatedRespect,
                    beneficiaryChallenge
            ));
            return;
        }

        NotificationCreateCommand obligatedChallenge = new NotificationCreateCommand(
                challenge.getObligatedUser().getId(),
                NotificationType.CHALLENGE_REJECTED,
                "Challenge marked not fulfilled",
                challenge.getBeneficiaryUser().getDisplayName() + " marked your challenge as not fulfilled.",
                navigationPath,
                challenge.getSourceRoom().getId(),
                challenge.getSourceGameSession().getId(),
                challenge.getId(),
                null,
                null,
                "challenge-rejected:" + challenge.getId()
        );
        NotificationCreateCommand obligatedKarma = new NotificationCreateCommand(
                challenge.getObligatedUser().getId(),
                NotificationType.KARMA_APPLIED,
                "Karma applied",
                "You received " + challenge.getKarmaPointsAwarded() + " Karma from challenge resolution.",
                navigationPath,
                challenge.getSourceRoom().getId(),
                challenge.getSourceGameSession().getId(),
                challenge.getId(),
                null,
                null,
                "karma-applied:" + challenge.getId()
        );
        NotificationCreateCommand beneficiaryChallenge = new NotificationCreateCommand(
                challenge.getBeneficiaryUser().getId(),
                NotificationType.CHALLENGE_REJECTED,
                "Challenge resolved",
                "You marked challenge outcome as not fulfilled for " + challenge.getObligatedUser().getDisplayName() + ".",
                navigationPath,
                challenge.getSourceRoom().getId(),
                challenge.getSourceGameSession().getId(),
                challenge.getId(),
                null,
                null,
                "challenge-rejected:" + challenge.getId()
        );
        notificationService.safeCreateNotifications(List.of(
                obligatedChallenge,
                obligatedKarma,
                beneficiaryChallenge
        ));
    }
}
