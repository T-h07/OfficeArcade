package com.officearcade.server.moderation;

import com.officearcade.server.challenges.ChallengeStatus;
import com.officearcade.server.challenges.dto.ChallengeSummaryResponse;
import com.officearcade.server.challenges.persistence.ChallengeTypeEntity;
import com.officearcade.server.challenges.persistence.ChallengeTypeEntityRepository;
import com.officearcade.server.challenges.persistence.PostMatchChallengeEntity;
import com.officearcade.server.challenges.persistence.PostMatchChallengeEntityRepository;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntity;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntityRepository;
import com.officearcade.server.identity.AppRole;
import com.officearcade.server.lobby.persistence.RoomEntity;
import com.officearcade.server.lobby.persistence.RoomEntityRepository;
import com.officearcade.server.notifications.NotificationCreateCommand;
import com.officearcade.server.notifications.NotificationService;
import com.officearcade.server.notifications.NotificationType;
import com.officearcade.server.moderation.dto.AdminChallengeResolutionRequest;
import com.officearcade.server.moderation.dto.AdminChallengeReviewListResponse;
import com.officearcade.server.moderation.dto.AdminChallengeReviewResponse;
import com.officearcade.server.moderation.dto.AdminDismissReportRequest;
import com.officearcade.server.moderation.dto.AdminReportActionRequest;
import com.officearcade.server.moderation.dto.ChallengePolicyListResponse;
import com.officearcade.server.moderation.dto.ChallengePolicySettingResponse;
import com.officearcade.server.moderation.dto.ChallengePolicyUpdateItemRequest;
import com.officearcade.server.moderation.dto.ChallengePolicyUpdateRequest;
import com.officearcade.server.moderation.dto.CreateModerationReportRequest;
import com.officearcade.server.moderation.dto.ModerationAuditEntryResponse;
import com.officearcade.server.moderation.dto.ModerationAuditListResponse;
import com.officearcade.server.moderation.dto.ModerationReportListResponse;
import com.officearcade.server.moderation.dto.ModerationReportResponse;
import com.officearcade.server.moderation.dto.ModerationUserStateResponse;
import com.officearcade.server.moderation.persistence.ModerationAuditLogEntity;
import com.officearcade.server.moderation.persistence.ModerationAuditLogEntityRepository;
import com.officearcade.server.moderation.persistence.ModerationReportEntity;
import com.officearcade.server.moderation.persistence.ModerationReportEntityRepository;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModerationService {

    private static final int REPORT_NOTE_MAX_LENGTH = 280;
    private static final int SUSPENSION_NOTE_MAX_LENGTH = 240;

    private final ModerationReportEntityRepository moderationReportEntityRepository;
    private final ModerationAuditLogEntityRepository moderationAuditLogEntityRepository;
    private final UserEntityRepository userEntityRepository;
    private final RoomEntityRepository roomEntityRepository;
    private final ConnectFourGameEntityRepository connectFourGameEntityRepository;
    private final PostMatchChallengeEntityRepository postMatchChallengeEntityRepository;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final ChallengeTypeEntityRepository challengeTypeEntityRepository;
    private final NotificationService notificationService;

    public ModerationService(
            ModerationReportEntityRepository moderationReportEntityRepository,
            ModerationAuditLogEntityRepository moderationAuditLogEntityRepository,
            UserEntityRepository userEntityRepository,
            RoomEntityRepository roomEntityRepository,
            ConnectFourGameEntityRepository connectFourGameEntityRepository,
            PostMatchChallengeEntityRepository postMatchChallengeEntityRepository,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            ChallengeTypeEntityRepository challengeTypeEntityRepository,
            NotificationService notificationService
    ) {
        this.moderationReportEntityRepository = moderationReportEntityRepository;
        this.moderationAuditLogEntityRepository = moderationAuditLogEntityRepository;
        this.userEntityRepository = userEntityRepository;
        this.roomEntityRepository = roomEntityRepository;
        this.connectFourGameEntityRepository = connectFourGameEntityRepository;
        this.postMatchChallengeEntityRepository = postMatchChallengeEntityRepository;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.challengeTypeEntityRepository = challengeTypeEntityRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ModerationReportResponse createReport(String reporterUserIdText, CreateModerationReportRequest request) {
        UUID reporterUserId = parseRequiredUuid(reporterUserIdText, "Invalid authenticated user context.");
        UUID reportedUserId = parseRequiredUuid(request.reportedUserId(), "Reported user id must be a valid UUID.");

        if (reporterUserId.equals(reportedUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot submit a report against your own account.");
        }

        UserEntity reporter = getRequiredUser(reporterUserId, "Reporter user not found.");
        UserEntity reported = getRequiredUser(reportedUserId, "Reported user not found.");

        ModerationReportEntity report = new ModerationReportEntity();
        report.setReporterUser(reporter);
        report.setReportedUser(reported);
        report.setCategory(request.category());
        report.setNote(normalizeOptionalNote(request.note(), REPORT_NOTE_MAX_LENGTH));
        report.setStatus(ModerationReportStatus.OPEN);
        report.setSourceRoom(resolveOptionalRoom(request.sourceRoomId()));
        report.setSourceGameSession(resolveOptionalGameSession(request.sourceGameSessionId()));
        report.setSourceChallenge(resolveOptionalChallenge(request.sourceChallengeId()));
        report.setReviewedByAdmin(null);
        report.setResolutionNote(null);

        ModerationReportEntity saved = moderationReportEntityRepository.save(report);
        return toReportResponse(saved);
    }

    @Transactional(readOnly = true)
    public ModerationReportListResponse listReportsForReporter(String reporterUserIdText) {
        UUID reporterUserId = parseRequiredUuid(reporterUserIdText, "Invalid authenticated user context.");
        List<ModerationReportResponse> reports = moderationReportEntityRepository.findAllByReporterUserId(reporterUserId).stream()
                .map(this::toReportResponse)
                .toList();
        return new ModerationReportListResponse(reports.size(), reports);
    }

    @Transactional(readOnly = true)
    public ModerationReportListResponse listReportsForAdmin(ModerationReportStatus status, ModerationReportCategory category) {
        Specification<ModerationReportEntity> specification = Specification.where(null);
        if (status != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("status"), status));
        } else {
            EnumSet<ModerationReportStatus> queueStatuses = EnumSet.of(ModerationReportStatus.OPEN, ModerationReportStatus.IN_REVIEW);
            specification = specification.and((root, query, criteriaBuilder) -> root.get("status").in(queueStatuses));
        }
        if (category != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("category"), category));
        }

        Sort sort = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
        List<ModerationReportResponse> reports = moderationReportEntityRepository.findAll(specification, sort).stream()
                .map(this::toReportResponse)
                .toList();
        return new ModerationReportListResponse(reports.size(), reports);
    }

    @Transactional
    public ModerationReportResponse getReportForAdmin(String reportIdText) {
        UUID reportId = parseRequiredUuid(reportIdText, "Report id must be a valid UUID.");
        ModerationReportEntity report = moderationReportEntityRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found: " + reportId));

        if (report.getStatus() == ModerationReportStatus.OPEN) {
            report.setStatus(ModerationReportStatus.IN_REVIEW);
            report = moderationReportEntityRepository.save(report);
        }

        return toReportResponse(report);
    }

    @Transactional
    public ModerationReportResponse dismissReport(String adminUserIdText, String reportIdText, AdminDismissReportRequest request) {
        UserEntity adminUser = getRequiredAdmin(adminUserIdText);
        ModerationReportEntity report = getRequiredReportForAction(reportIdText);

        report.setStatus(ModerationReportStatus.DISMISSED);
        report.setReviewedByAdmin(adminUser);
        report.setResolutionNote(normalizeOptionalNote(request.note(), REPORT_NOTE_MAX_LENGTH));
        ModerationReportEntity saved = moderationReportEntityRepository.save(report);

        appendAuditLog(
                adminUser,
                report.getReportedUser(),
                ModerationAuditActionType.REPORT_DISMISSED,
                saved,
                null,
                saved.getResolutionNote()
        );
        notifyReporterReportStatus(saved);

        return toReportResponse(saved);
    }

    @Transactional
    public ModerationReportResponse takeReportAction(String adminUserIdText, String reportIdText, AdminReportActionRequest request) {
        UserEntity adminUser = getRequiredAdmin(adminUserIdText);
        ModerationReportEntity report = getRequiredReportForAction(reportIdText);
        String note = normalizeOptionalNote(request.note(), REPORT_NOTE_MAX_LENGTH);

        switch (request.actionType()) {
            case NOTE_ONLY -> appendAuditLog(
                    adminUser,
                    report.getReportedUser(),
                    ModerationAuditActionType.REPORT_NOTE_ONLY,
                    report,
                    null,
                    note
            );
            case SUSPEND_USER -> applySuspension(adminUser, report.getReportedUser(), true, note, report);
            case UNSUSPEND_USER -> applySuspension(adminUser, report.getReportedUser(), false, note, report);
        }

        report.setStatus(ModerationReportStatus.RESOLVED);
        report.setReviewedByAdmin(adminUser);
        report.setResolutionNote(note);
        ModerationReportEntity saved = moderationReportEntityRepository.save(report);
        notifyReporterReportStatus(saved);

        return toReportResponse(saved);
    }

    @Transactional
    public ModerationUserStateResponse suspendUser(String adminUserIdText, String targetUserIdText, String note) {
        UserEntity adminUser = getRequiredAdmin(adminUserIdText);
        UUID targetUserId = parseRequiredUuid(targetUserIdText, "User id must be a valid UUID.");
        UserEntity targetUser = getRequiredUser(targetUserId, "User not found: " + targetUserIdText);
        applySuspension(
                adminUser,
                targetUser,
                true,
                normalizeOptionalNote(note, SUSPENSION_NOTE_MAX_LENGTH),
                null
        );
        return toModerationUserStateResponse(targetUser);
    }

    @Transactional
    public ModerationUserStateResponse unsuspendUser(String adminUserIdText, String targetUserIdText, String note) {
        UserEntity adminUser = getRequiredAdmin(adminUserIdText);
        UUID targetUserId = parseRequiredUuid(targetUserIdText, "User id must be a valid UUID.");
        UserEntity targetUser = getRequiredUser(targetUserId, "User not found: " + targetUserIdText);
        applySuspension(
                adminUser,
                targetUser,
                false,
                normalizeOptionalNote(note, SUSPENSION_NOTE_MAX_LENGTH),
                null
        );
        return toModerationUserStateResponse(targetUser);
    }

    @Transactional(readOnly = true)
    public AdminChallengeReviewListResponse listDisputedChallenges() {
        List<AdminChallengeReviewResponse> responses = postMatchChallengeEntityRepository
                .findAllByStatusOrderByCreatedAtAsc(ChallengeStatus.DISPUTED).stream()
                .map(this::toAdminChallengeReviewResponse)
                .toList();
        return new AdminChallengeReviewListResponse(responses.size(), responses);
    }

    @Transactional
    public ChallengeSummaryResponse resolveDisputedChallenge(
            String adminUserIdText,
            String challengeIdText,
            AdminChallengeResolutionRequest request
    ) {
        UserEntity adminUser = getRequiredAdmin(adminUserIdText);
        UUID challengeId = parseRequiredUuid(challengeIdText, "Challenge id must be a valid UUID.");
        PostMatchChallengeEntity challenge = postMatchChallengeEntityRepository.findByIdForUpdate(challengeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Challenge not found: " + challengeId));

        if (challenge.getStatus() != ChallengeStatus.DISPUTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Challenge is not in disputed review state.");
        }

        PlayerProfileEntity obligatedProfile = getRequiredProfile(challenge.getObligatedUser().getId());
        ChallengeTypeEntity challengeType = challenge.getChallengeType();

        ChallengeStatus nextStatus;
        if (request.resolutionType() == ChallengeDisputeResolutionType.CONFIRM_COMPLETED) {
            int respectAward = Math.max(challengeType.getRespectRewardPoints(), 0);
            obligatedProfile.setRespectPoints(obligatedProfile.getRespectPoints() + respectAward);
            challenge.setRespectPointsAwarded(respectAward);
            challenge.setKarmaPointsAwarded(0);
            nextStatus = ChallengeStatus.COMPLETED_CONFIRMED;
            playerProfileEntityRepository.save(obligatedProfile);
        } else if (request.resolutionType() == ChallengeDisputeResolutionType.REJECT_NOT_FULFILLED) {
            int karmaAward = Math.max(challengeType.getKarmaPenaltyPoints(), 0);
            obligatedProfile.setKarmaPoints(obligatedProfile.getKarmaPoints() + karmaAward);
            challenge.setRespectPointsAwarded(0);
            challenge.setKarmaPointsAwarded(karmaAward);
            nextStatus = ChallengeStatus.REJECTED;
            playerProfileEntityRepository.save(obligatedProfile);
        } else {
            challenge.setRespectPointsAwarded(0);
            challenge.setKarmaPointsAwarded(0);
            nextStatus = ChallengeStatus.CANCELLED;
        }

        challenge.setStatus(nextStatus);
        challenge.setResolvedAt(Instant.now());
        challenge.setResolvedByAdmin(adminUser);
        challenge.setResolutionNote(normalizeOptionalNote(request.note(), REPORT_NOTE_MAX_LENGTH));
        PostMatchChallengeEntity saved = postMatchChallengeEntityRepository.save(challenge);

        appendAuditLog(
                adminUser,
                challenge.getObligatedUser(),
                ModerationAuditActionType.CHALLENGE_DISPUTE_RESOLVED,
                null,
                saved,
                challenge.getResolutionNote()
        );
        notifyChallengeDisputeResolved(saved);

        return toChallengeSummaryResponse(saved);
    }

    @Transactional(readOnly = true)
    public ModerationAuditListResponse listAuditEntries() {
        List<ModerationAuditEntryResponse> entries = moderationAuditLogEntityRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toAuditEntryResponse)
                .toList();
        return new ModerationAuditListResponse(entries.size(), entries);
    }

    @Transactional(readOnly = true)
    public ChallengePolicyListResponse listChallengePolicies() {
        List<ChallengePolicySettingResponse> policies = challengeTypeEntityRepository.findAll(Sort.by(Sort.Order.asc("displayName"))).stream()
                .map(this::toChallengePolicySettingResponse)
                .toList();
        return new ChallengePolicyListResponse(policies.size(), policies);
    }

    @Transactional
    public ChallengePolicyListResponse updateChallengePolicies(String adminUserIdText, ChallengePolicyUpdateRequest request) {
        UserEntity adminUser = getRequiredAdmin(adminUserIdText);
        List<ChallengeTypeEntity> allPolicies = new ArrayList<>(challengeTypeEntityRepository.findAll(Sort.by(Sort.Order.asc("displayName"))));
        if (allPolicies.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No challenge policy rows are available to update.");
        }

        Map<String, ChallengePolicyUpdateItemRequest> updatesByCode = new LinkedHashMap<>();
        for (ChallengePolicyUpdateItemRequest update : request.policies()) {
            String normalizedCode = normalizePolicyCode(update.code());
            if (updatesByCode.containsKey(normalizedCode)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate challenge policy update code: " + update.code()
                );
            }
            updatesByCode.put(normalizedCode, update);
        }

        List<String> unknownCodes = new ArrayList<>();
        for (String code : updatesByCode.keySet()) {
            boolean exists = allPolicies.stream().anyMatch(policy -> policy.getCode().equalsIgnoreCase(code));
            if (!exists) {
                unknownCodes.add(code);
            }
        }
        if (!unknownCodes.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Unknown challenge policy codes: " + String.join(", ", unknownCodes)
            );
        }

        List<String> changedCodes = new ArrayList<>();
        for (ChallengeTypeEntity policy : allPolicies) {
            ChallengePolicyUpdateItemRequest update = updatesByCode.get(normalizePolicyCode(policy.getCode()));
            if (update == null) {
                continue;
            }
            if (policy.isEnabled() != update.enabled()) {
                policy.setEnabled(update.enabled());
                changedCodes.add(policy.getCode());
            }
        }

        boolean hasAtLeastOneEnabled = allPolicies.stream().anyMatch(ChallengeTypeEntity::isEnabled);
        if (!hasAtLeastOneEnabled) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "At least one challenge type must remain enabled."
            );
        }

        if (!changedCodes.isEmpty()) {
            challengeTypeEntityRepository.saveAll(allPolicies);
            appendAuditLog(
                    adminUser,
                    null,
                    ModerationAuditActionType.CHALLENGE_POLICY_UPDATED,
                    null,
                    null,
                    "Updated policies: " + String.join(", ", changedCodes)
            );
        }

        return listChallengePolicies();
    }

    private ModerationReportEntity getRequiredReportForAction(String reportIdText) {
        UUID reportId = parseRequiredUuid(reportIdText, "Report id must be a valid UUID.");
        ModerationReportEntity report = moderationReportEntityRepository.findByIdForUpdate(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found: " + reportId));
        if (report.getStatus() == ModerationReportStatus.RESOLVED || report.getStatus() == ModerationReportStatus.DISMISSED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report is already finalized.");
        }
        return report;
    }

    private void applySuspension(
            UserEntity adminUser,
            UserEntity targetUser,
            boolean suspended,
            String note,
            ModerationReportEntity report
    ) {
        if (suspended) {
            if (targetUser.isSuspended()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already suspended.");
            }
            assertCanSuspendTargetUser(targetUser);
            targetUser.setSuspended(true);
            targetUser.setSuspendedAt(Instant.now());
            targetUser.setSuspensionNote(normalizeOptionalNote(note, SUSPENSION_NOTE_MAX_LENGTH));
            targetUser.setSuspendedByAdminId(adminUser.getId());
            userEntityRepository.save(targetUser);
            appendAuditLog(
                    adminUser,
                    targetUser,
                    ModerationAuditActionType.USER_SUSPENDED,
                    report,
                    null,
                    targetUser.getSuspensionNote()
            );
            notifyModerationStatusChanged(targetUser, true);
            return;
        }

        if (!targetUser.isSuspended()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is not currently suspended.");
        }

        targetUser.setSuspended(false);
        targetUser.setSuspendedAt(null);
        targetUser.setSuspensionNote(null);
        targetUser.setSuspendedByAdminId(null);
        userEntityRepository.save(targetUser);
        appendAuditLog(
                adminUser,
                targetUser,
                ModerationAuditActionType.USER_UNSUSPENDED,
                report,
                null,
                note
        );
        notifyModerationStatusChanged(targetUser, false);
    }

    private void assertCanSuspendTargetUser(UserEntity targetUser) {
        if (targetUser.getRole() != AppRole.ADMIN || !targetUser.isEnabled()) {
            return;
        }

        long activeAdminCount = userEntityRepository.countByRoleAndEnabledTrueAndSuspendedFalse(AppRole.ADMIN);
        if (activeAdminCount <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot suspend the last active ADMIN account."
            );
        }
    }

    private UserEntity getRequiredAdmin(String adminUserIdText) {
        UUID adminUserId = parseRequiredUuid(adminUserIdText, "Invalid authenticated user context.");
        UserEntity adminUser = getRequiredUser(adminUserId, "Admin user not found.");
        if (adminUser.getRole() != AppRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin moderation access is required.");
        }
        return adminUser;
    }

    private UserEntity getRequiredUser(UUID userId, String notFoundMessage) {
        return userEntityRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, notFoundMessage));
    }

    private RoomEntity resolveOptionalRoom(String roomIdText) {
        UUID roomId = parseOptionalUuid(roomIdText);
        if (roomId == null) {
            return null;
        }
        return roomEntityRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room not found: " + roomId));
    }

    private ConnectFourGameEntity resolveOptionalGameSession(String sourceGameSessionIdText) {
        UUID gameSessionId = parseOptionalUuid(sourceGameSessionIdText);
        if (gameSessionId == null) {
            return null;
        }
        return connectFourGameEntityRepository.findById(gameSessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Game session not found: " + gameSessionId
                ));
    }

    private PostMatchChallengeEntity resolveOptionalChallenge(String sourceChallengeIdText) {
        UUID challengeId = parseOptionalUuid(sourceChallengeIdText);
        if (challengeId == null) {
            return null;
        }
        return postMatchChallengeEntityRepository.findById(challengeId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Challenge not found: " + challengeId
                ));
    }

    private static UUID parseRequiredUuid(String value, String errorMessage) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    private static UUID parseOptionalUuid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid UUID value: " + value);
        }
    }

    private static String normalizeOptionalNote(String note, int maxLength) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }

    private static String normalizePolicyCode(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }

    private PlayerProfileEntity getRequiredProfile(UUID userId) {
        return playerProfileEntityRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player profile is missing for user: " + userId
                ));
    }

    private void appendAuditLog(
            UserEntity adminUser,
            UserEntity targetUser,
            ModerationAuditActionType actionType,
            ModerationReportEntity report,
            PostMatchChallengeEntity challenge,
            String note
    ) {
        ModerationAuditLogEntity audit = new ModerationAuditLogEntity();
        audit.setAdminUser(adminUser);
        audit.setTargetUser(targetUser);
        audit.setActionType(actionType);
        audit.setReport(report);
        audit.setChallenge(challenge);
        audit.setNote(normalizeOptionalNote(note, REPORT_NOTE_MAX_LENGTH));
        moderationAuditLogEntityRepository.save(audit);
    }

    private ModerationReportResponse toReportResponse(ModerationReportEntity report) {
        return new ModerationReportResponse(
                report.getId().toString(),
                report.getReporterUser().getId().toString(),
                report.getReporterUser().getDisplayName(),
                report.getReportedUser().getId().toString(),
                report.getReportedUser().getDisplayName(),
                report.getCategory().name(),
                report.getNote(),
                report.getStatus().name(),
                nullableRoomId(report.getSourceRoom()),
                nullableGameId(report.getSourceGameSession()),
                nullableChallengeId(report.getSourceChallenge()),
                nullableUserId(report.getReviewedByAdmin()),
                report.getReviewedByAdmin() == null ? null : report.getReviewedByAdmin().getDisplayName(),
                report.getResolutionNote(),
                report.getCreatedAt().toString(),
                report.getUpdatedAt().toString()
        );
    }

    private AdminChallengeReviewResponse toAdminChallengeReviewResponse(PostMatchChallengeEntity challenge) {
        return new AdminChallengeReviewResponse(
                challenge.getId().toString(),
                challenge.getSourceRoom().getId().toString(),
                challenge.getSourceGameSession().getId().toString(),
                challenge.getChallengeType().getCode(),
                challenge.getChallengeType().getDisplayName(),
                challenge.getStatus().name(),
                challenge.getObligatedUser().getId().toString(),
                challenge.getObligatedUser().getDisplayName(),
                challenge.getBeneficiaryUser().getId().toString(),
                challenge.getBeneficiaryUser().getDisplayName(),
                challenge.getDisputeNote(),
                nullableInstant(challenge.getDisputedAt()),
                challenge.getCreatedAt().toString()
        );
    }

    private ModerationAuditEntryResponse toAuditEntryResponse(ModerationAuditLogEntity entry) {
        return new ModerationAuditEntryResponse(
                entry.getId().toString(),
                entry.getAdminUser().getId().toString(),
                entry.getAdminUser().getDisplayName(),
                nullableUserId(entry.getTargetUser()),
                entry.getTargetUser() == null ? null : entry.getTargetUser().getDisplayName(),
                entry.getActionType().name(),
                nullableReportId(entry.getReport()),
                nullableChallengeId(entry.getChallenge()),
                entry.getNote(),
                entry.getCreatedAt().toString()
        );
    }

    private ChallengePolicySettingResponse toChallengePolicySettingResponse(ChallengeTypeEntity policy) {
        return new ChallengePolicySettingResponse(
                policy.getCode(),
                policy.getDisplayName(),
                policy.getDescription(),
                policy.getRespectRewardPoints(),
                policy.getKarmaPenaltyPoints(),
                policy.isEnabled(),
                policy.getUpdatedAt().toString()
        );
    }

    private ModerationUserStateResponse toModerationUserStateResponse(UserEntity user) {
        return new ModerationUserStateResponse(
                user.getId().toString(),
                user.getDisplayName(),
                user.isSuspended(),
                nullableInstant(user.getSuspendedAt()),
                user.getSuspensionNote()
        );
    }

    private ChallengeSummaryResponse toChallengeSummaryResponse(PostMatchChallengeEntity challenge) {
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
                "ADMIN_REVIEW",
                false,
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

    private static String nullableInstant(Instant value) {
        return value == null ? null : value.toString();
    }

    private static String nullableUserId(UserEntity user) {
        return user == null ? null : user.getId().toString();
    }

    private static String nullableRoomId(RoomEntity room) {
        return room == null ? null : room.getId().toString();
    }

    private static String nullableGameId(ConnectFourGameEntity game) {
        return game == null ? null : game.getId().toString();
    }

    private static String nullableChallengeId(PostMatchChallengeEntity challenge) {
        return challenge == null ? null : challenge.getId().toString();
    }

    private static String nullableReportId(ModerationReportEntity report) {
        return report == null ? null : report.getId().toString();
    }

    private void notifyReporterReportStatus(ModerationReportEntity report) {
        String message = report.getStatus() == ModerationReportStatus.DISMISSED
                ? "Your report was reviewed and dismissed."
                : "Your report was reviewed and action was recorded.";

        notificationService.safeCreateNotification(new NotificationCreateCommand(
                report.getReporterUser().getId(),
                NotificationType.REPORT_STATUS_UPDATE,
                "Report status updated",
                message,
                "/app/notifications",
                nullableUuid(report.getSourceRoom()),
                nullableUuid(report.getSourceGameSession()),
                nullableUuid(report.getSourceChallenge()),
                null,
                report.getId(),
                "report-status:" + report.getId() + ":" + report.getStatus().name()
        ));
    }

    private void notifyModerationStatusChanged(UserEntity targetUser, boolean suspended) {
        String message = suspended
                ? "Your account status has been updated. Access is currently suspended."
                : "Your account access has been restored.";

        notificationService.safeCreateNotification(new NotificationCreateCommand(
                targetUser.getId(),
                NotificationType.MODERATION_STATUS_UPDATE,
                "Account status updated",
                message,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    private void notifyChallengeDisputeResolved(PostMatchChallengeEntity challenge) {
        String navigationPath = "/app/challenges";
        String message;
        if (challenge.getStatus() == ChallengeStatus.COMPLETED_CONFIRMED) {
            message = "A disputed challenge was confirmed by admin review.";
            notificationService.safeCreateNotification(new NotificationCreateCommand(
                    challenge.getObligatedUser().getId(),
                    NotificationType.RESPECT_GAINED,
                    "Respect awarded",
                    "Admin review awarded " + challenge.getRespectPointsAwarded() + " Respect.",
                    navigationPath,
                    challenge.getSourceRoom().getId(),
                    challenge.getSourceGameSession().getId(),
                    challenge.getId(),
                    null,
                    null,
                    "respect-gained-admin:" + challenge.getId()
            ));
        } else if (challenge.getStatus() == ChallengeStatus.REJECTED) {
            message = "A disputed challenge was marked not fulfilled by admin review.";
            notificationService.safeCreateNotification(new NotificationCreateCommand(
                    challenge.getObligatedUser().getId(),
                    NotificationType.KARMA_APPLIED,
                    "Karma applied",
                    "Admin review applied " + challenge.getKarmaPointsAwarded() + " Karma.",
                    navigationPath,
                    challenge.getSourceRoom().getId(),
                    challenge.getSourceGameSession().getId(),
                    challenge.getId(),
                    null,
                    null,
                    "karma-applied-admin:" + challenge.getId()
            ));
        } else {
            message = "A disputed challenge was canceled without points by admin review.";
        }

        NotificationCreateCommand obligatedNotification = new NotificationCreateCommand(
                challenge.getObligatedUser().getId(),
                NotificationType.CHALLENGE_DISPUTE_RESOLVED,
                "Challenge dispute resolved",
                message,
                navigationPath,
                challenge.getSourceRoom().getId(),
                challenge.getSourceGameSession().getId(),
                challenge.getId(),
                null,
                null,
                "challenge-dispute-resolved:" + challenge.getId() + ":" + challenge.getStatus().name()
        );

        NotificationCreateCommand beneficiaryNotification = new NotificationCreateCommand(
                challenge.getBeneficiaryUser().getId(),
                NotificationType.CHALLENGE_DISPUTE_RESOLVED,
                "Challenge dispute resolved",
                message,
                navigationPath,
                challenge.getSourceRoom().getId(),
                challenge.getSourceGameSession().getId(),
                challenge.getId(),
                null,
                null,
                "challenge-dispute-resolved:" + challenge.getId() + ":" + challenge.getStatus().name()
        );

        notificationService.safeCreateNotifications(List.of(obligatedNotification, beneficiaryNotification));
    }

    private static UUID nullableUuid(RoomEntity roomEntity) {
        return roomEntity == null ? null : roomEntity.getId();
    }

    private static UUID nullableUuid(ConnectFourGameEntity gameEntity) {
        return gameEntity == null ? null : gameEntity.getId();
    }

    private static UUID nullableUuid(PostMatchChallengeEntity challengeEntity) {
        return challengeEntity == null ? null : challengeEntity.getId();
    }
}
