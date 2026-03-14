package com.officearcade.server.admin.analytics;

import com.officearcade.server.admin.analytics.dto.AdminAnalyticsDashboardResponse;
import com.officearcade.server.admin.analytics.dto.AnalyticsActivityPointResponse;
import com.officearcade.server.admin.analytics.dto.AnalyticsCategoryCountResponse;
import com.officearcade.server.admin.analytics.dto.AnalyticsDepartmentInsightResponse;
import com.officearcade.server.admin.analytics.dto.AnalyticsFilterResponse;
import com.officearcade.server.admin.analytics.dto.AnalyticsGameUsageResponse;
import com.officearcade.server.admin.analytics.dto.AnalyticsModerationResponse;
import com.officearcade.server.admin.analytics.dto.AnalyticsReputationResponse;
import com.officearcade.server.admin.analytics.dto.AnalyticsSummaryResponse;
import com.officearcade.server.challenges.ChallengeStatus;
import com.officearcade.server.challenges.persistence.PostMatchChallengeEntity;
import com.officearcade.server.challenges.persistence.PostMatchChallengeEntityRepository;
import com.officearcade.server.departments.dto.DepartmentSummaryResponse;
import com.officearcade.server.departments.persistence.DepartmentEntity;
import com.officearcade.server.departments.persistence.DepartmentEntityRepository;
import com.officearcade.server.games.connectfour.ConnectFourGameStatus;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntity;
import com.officearcade.server.games.connectfour.persistence.ConnectFourGameEntityRepository;
import com.officearcade.server.games.trivia.TriviaGameStatus;
import com.officearcade.server.games.trivia.persistence.TriviaGameEntity;
import com.officearcade.server.games.trivia.persistence.TriviaGameEntityRepository;
import com.officearcade.server.lobby.persistence.RoomEntity;
import com.officearcade.server.lobby.persistence.RoomEntityRepository;
import com.officearcade.server.moderation.ModerationReportStatus;
import com.officearcade.server.moderation.persistence.ModerationReportEntity;
import com.officearcade.server.moderation.persistence.ModerationReportEntityRepository;
import com.officearcade.server.playlimits.PlayLimitPolicyProperties;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAnalyticsService {

    private static final String UNASSIGNED_BUCKET_KEY = "UNASSIGNED";
    private static final String UNASSIGNED_BUCKET_CODE = "UNASSIGNED";
    private static final String UNASSIGNED_BUCKET_NAME = "Unassigned";

    private final UserEntityRepository userEntityRepository;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final RoomEntityRepository roomEntityRepository;
    private final ConnectFourGameEntityRepository connectFourGameEntityRepository;
    private final TriviaGameEntityRepository triviaGameEntityRepository;
    private final PostMatchChallengeEntityRepository postMatchChallengeEntityRepository;
    private final ModerationReportEntityRepository moderationReportEntityRepository;
    private final DepartmentEntityRepository departmentEntityRepository;
    private final PlayLimitPolicyProperties playLimitPolicyProperties;

    public AdminAnalyticsService(
            UserEntityRepository userEntityRepository,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            RoomEntityRepository roomEntityRepository,
            ConnectFourGameEntityRepository connectFourGameEntityRepository,
            TriviaGameEntityRepository triviaGameEntityRepository,
            PostMatchChallengeEntityRepository postMatchChallengeEntityRepository,
            ModerationReportEntityRepository moderationReportEntityRepository,
            DepartmentEntityRepository departmentEntityRepository,
            PlayLimitPolicyProperties playLimitPolicyProperties
    ) {
        this.userEntityRepository = userEntityRepository;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.roomEntityRepository = roomEntityRepository;
        this.connectFourGameEntityRepository = connectFourGameEntityRepository;
        this.triviaGameEntityRepository = triviaGameEntityRepository;
        this.postMatchChallengeEntityRepository = postMatchChallengeEntityRepository;
        this.moderationReportEntityRepository = moderationReportEntityRepository;
        this.departmentEntityRepository = departmentEntityRepository;
        this.playLimitPolicyProperties = playLimitPolicyProperties;
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsDashboardResponse getDashboard(String rangeQuery, String departmentFilterQuery) {
        Instant now = Instant.now();
        ZoneId zoneId = playLimitPolicyProperties.zoneId();
        LocalDate today = LocalDate.ofInstant(now, zoneId);
        Instant todayStart = today.atStartOfDay(zoneId).toInstant();

        AnalyticsRange range = AnalyticsRange.fromQuery(rangeQuery);
        Instant rangeStart = range.resolveRangeStart(now, zoneId);
        LocalDate trendStartDate = range.resolveTrendStart(today);
        Instant trendStartInstant = trendStartDate.atStartOfDay(zoneId).toInstant();

        List<UserEntity> users = userEntityRepository.findAll();
        Map<UUID, UserEntity> userById = new HashMap<>();
        for (UserEntity user : users) {
            userById.put(user.getId(), user);
        }

        DepartmentScope scope = resolveDepartmentScope(departmentFilterQuery, users);
        Set<UUID> scopedUserIds = scope.scopedUserIds();

        List<DepartmentEntity> departments = departmentEntityRepository
                .findAll(Sort.by(Sort.Order.asc("displayName"), Sort.Order.asc("code")));
        Map<String, DepartmentAccumulator> departmentAccumulators = initializeDepartmentAccumulators(scope, departments);

        Map<UUID, PlayerProfileEntity> profileByUserId = new HashMap<>();
        for (PlayerProfileEntity profile : playerProfileEntityRepository.findAll()) {
            profileByUserId.put(profile.getUserId(), profile);
        }

        int totalEnabledUsers = 0;
        int suspendedUsers = 0;
        for (UserEntity user : users) {
            if (!scope.scopeAll() && !scopedUserIds.contains(user.getId())) {
                continue;
            }
            if (user.isEnabled()) {
                totalEnabledUsers += 1;
            }
            if (user.isSuspended()) {
                suspendedUsers += 1;
            }

            String bucketKey = departmentBucketKey(user);
            DepartmentAccumulator accumulator = departmentAccumulators.get(bucketKey);
            if (accumulator == null) {
                continue;
            }

            accumulator.userCount += 1;
            PlayerProfileEntity profile = profileByUserId.get(user.getId());
            if (profile != null) {
                accumulator.profileCount += 1;
                accumulator.totalRespect += Math.max(profile.getRespectPoints(), 0);
                accumulator.totalKarma += Math.max(profile.getKarmaPoints(), 0);
            }
        }

        Set<UUID> activeUsersInRange = new HashSet<>();
        Set<UUID> activeUsersToday = new HashSet<>();
        Map<LocalDate, Set<UUID>> activeUsersByDay = new HashMap<>();
        Map<LocalDate, Integer> roomsCreatedByDay = new HashMap<>();
        Map<LocalDate, Integer> matchesByDay = new HashMap<>();
        Map<String, Integer> matchParticipationsByDepartment = new HashMap<>();
        Map<String, GameCounter> gameCounters = new HashMap<>();

        int roomsCreatedInRange = 0;
        for (RoomEntity room : roomEntityRepository.findAll()) {
            UUID hostUserId = room.getHostUser() == null ? null : room.getHostUser().getId();
            if (hostUserId == null) {
                continue;
            }
            if (!scope.scopeAll() && !scopedUserIds.contains(hostUserId)) {
                continue;
            }

            Instant createdAt = room.getCreatedAt();
            if (createdAt == null || createdAt.isAfter(now)) {
                continue;
            }

            if (isInRange(createdAt, rangeStart, now)) {
                roomsCreatedInRange += 1;
                activeUsersInRange.add(hostUserId);
            }
            if (!createdAt.isBefore(todayStart)) {
                activeUsersToday.add(hostUserId);
            }

            if (!createdAt.isBefore(trendStartInstant)) {
                LocalDate eventDate = LocalDate.ofInstant(createdAt, zoneId);
                roomsCreatedByDay.merge(eventDate, 1, Integer::sum);
                activeUsersByDay.computeIfAbsent(eventDate, ignored -> new HashSet<>()).add(hostUserId);
            }
        }

        int matchesInRange = 0;
        int matchesToday = 0;

        for (ConnectFourGameEntity game : connectFourGameEntityRepository.findAll()) {
            if (game.getStatus() != ConnectFourGameStatus.FINISHED) {
                continue;
            }

            Instant eventTime = resolveEventTime(game.getEndedAt(), game.getUpdatedAt(), game.getCreatedAt());
            if (eventTime == null || eventTime.isAfter(now)) {
                continue;
            }

            List<UUID> participantIds = participants(
                    game.getPlayerOneUser() == null ? null : game.getPlayerOneUser().getId(),
                    game.getPlayerTwoUser() == null ? null : game.getPlayerTwoUser().getId()
            );
            if (!isMatchInScope(participantIds, scope.scopeAll(), scopedUserIds)) {
                continue;
            }

            if (isInRange(eventTime, rangeStart, now)) {
                matchesInRange += 1;
                if (!eventTime.isBefore(todayStart)) {
                    matchesToday += 1;
                }

                String gameTypeCode = gameTypeCode(game.getRoom(), "CONNECT_FOUR");
                String gameTypeDisplayName = gameTypeDisplayName(game.getRoom(), "Connect Four");
                gameCounters.computeIfAbsent(gameTypeCode, ignored -> new GameCounter(gameTypeCode, gameTypeDisplayName))
                        .matches += 1;

                for (UUID participantId : participantIds) {
                    if (!scope.scopeAll() && !scopedUserIds.contains(participantId)) {
                        continue;
                    }
                    activeUsersInRange.add(participantId);
                    if (!eventTime.isBefore(todayStart)) {
                        activeUsersToday.add(participantId);
                    }
                    UserEntity participant = userById.get(participantId);
                    String bucketKey = departmentBucketKey(participant);
                    matchParticipationsByDepartment.merge(bucketKey, 1, Integer::sum);
                }
            }

            if (!eventTime.isBefore(trendStartInstant)) {
                LocalDate eventDate = LocalDate.ofInstant(eventTime, zoneId);
                matchesByDay.merge(eventDate, 1, Integer::sum);
                for (UUID participantId : participantIds) {
                    if (!scope.scopeAll() && !scopedUserIds.contains(participantId)) {
                        continue;
                    }
                    activeUsersByDay.computeIfAbsent(eventDate, ignored -> new HashSet<>()).add(participantId);
                }
            }
        }

        for (TriviaGameEntity game : triviaGameEntityRepository.findAll()) {
            if (game.getStatus() != TriviaGameStatus.FINISHED) {
                continue;
            }

            Instant eventTime = resolveEventTime(game.getEndedAt(), game.getUpdatedAt(), game.getCreatedAt());
            if (eventTime == null || eventTime.isAfter(now)) {
                continue;
            }

            List<UUID> participantIds = participants(
                    game.getPlayerOneUser() == null ? null : game.getPlayerOneUser().getId(),
                    game.getPlayerTwoUser() == null ? null : game.getPlayerTwoUser().getId()
            );
            if (!isMatchInScope(participantIds, scope.scopeAll(), scopedUserIds)) {
                continue;
            }

            if (isInRange(eventTime, rangeStart, now)) {
                matchesInRange += 1;
                if (!eventTime.isBefore(todayStart)) {
                    matchesToday += 1;
                }

                String gameTypeCode = gameTypeCode(game.getRoom(), "TRIVIA");
                String gameTypeDisplayName = gameTypeDisplayName(game.getRoom(), "Trivia Battle");
                gameCounters.computeIfAbsent(gameTypeCode, ignored -> new GameCounter(gameTypeCode, gameTypeDisplayName))
                        .matches += 1;

                for (UUID participantId : participantIds) {
                    if (!scope.scopeAll() && !scopedUserIds.contains(participantId)) {
                        continue;
                    }
                    activeUsersInRange.add(participantId);
                    if (!eventTime.isBefore(todayStart)) {
                        activeUsersToday.add(participantId);
                    }
                    UserEntity participant = userById.get(participantId);
                    String bucketKey = departmentBucketKey(participant);
                    matchParticipationsByDepartment.merge(bucketKey, 1, Integer::sum);
                }
            }

            if (!eventTime.isBefore(trendStartInstant)) {
                LocalDate eventDate = LocalDate.ofInstant(eventTime, zoneId);
                matchesByDay.merge(eventDate, 1, Integer::sum);
                for (UUID participantId : participantIds) {
                    if (!scope.scopeAll() && !scopedUserIds.contains(participantId)) {
                        continue;
                    }
                    activeUsersByDay.computeIfAbsent(eventDate, ignored -> new HashSet<>()).add(participantId);
                }
            }
        }

        int challengesCreatedInRange = 0;
        int challengesPending = 0;
        int challengesDisputed = 0;
        int confirmedChallengesInRange = 0;
        int rejectedChallengesInRange = 0;
        int respectAwardedInRange = 0;
        int karmaAppliedInRange = 0;

        for (PostMatchChallengeEntity challenge : postMatchChallengeEntityRepository.findAll()) {
            UUID obligatedUserId = challenge.getObligatedUser() == null ? null : challenge.getObligatedUser().getId();
            UUID beneficiaryUserId = challenge.getBeneficiaryUser() == null ? null : challenge.getBeneficiaryUser().getId();
            if (!isChallengeInScope(obligatedUserId, beneficiaryUserId, scope.scopeAll(), scopedUserIds)) {
                continue;
            }

            if (isInRange(challenge.getCreatedAt(), rangeStart, now)) {
                challengesCreatedInRange += 1;
            }

            if (challenge.getStatus() == ChallengeStatus.PENDING) {
                challengesPending += 1;
            } else if (challenge.getStatus() == ChallengeStatus.DISPUTED) {
                challengesDisputed += 1;
            }

            Instant resolvedTime = resolveEventTime(challenge.getResolvedAt(), challenge.getUpdatedAt(), challenge.getCreatedAt());
            if (challenge.getStatus() == ChallengeStatus.COMPLETED_CONFIRMED && isInRange(resolvedTime, rangeStart, now)) {
                confirmedChallengesInRange += 1;
                respectAwardedInRange += Math.max(challenge.getRespectPointsAwarded(), 0);
            } else if (challenge.getStatus() == ChallengeStatus.REJECTED && isInRange(resolvedTime, rangeStart, now)) {
                rejectedChallengesInRange += 1;
                karmaAppliedInRange += Math.max(challenge.getKarmaPointsAwarded(), 0);
            }
        }

        int openReports = 0;
        int inReviewReports = 0;
        int resolvedReports = 0;
        int dismissedReports = 0;
        int reportsCreatedInRange = 0;
        Map<String, Integer> reportCategoryCountsInRange = new HashMap<>();

        for (ModerationReportEntity report : moderationReportEntityRepository.findAll()) {
            UUID reportedUserId = report.getReportedUser() == null ? null : report.getReportedUser().getId();
            if (reportedUserId == null) {
                continue;
            }
            if (!scope.scopeAll() && !scopedUserIds.contains(reportedUserId)) {
                continue;
            }

            if (report.getStatus() == ModerationReportStatus.OPEN) {
                openReports += 1;
            } else if (report.getStatus() == ModerationReportStatus.IN_REVIEW) {
                inReviewReports += 1;
            } else if (report.getStatus() == ModerationReportStatus.RESOLVED) {
                resolvedReports += 1;
            } else if (report.getStatus() == ModerationReportStatus.DISMISSED) {
                dismissedReports += 1;
            }

            Instant createdAt = report.getCreatedAt();
            if (isInRange(createdAt, rangeStart, now)) {
                reportsCreatedInRange += 1;
                if (report.getCategory() != null) {
                    reportCategoryCountsInRange.merge(report.getCategory().name(), 1, Integer::sum);
                }
            }
        }

        for (Map.Entry<String, Integer> entry : matchParticipationsByDepartment.entrySet()) {
            DepartmentAccumulator accumulator = departmentAccumulators.get(entry.getKey());
            if (accumulator != null) {
                accumulator.matchParticipationsInRange = entry.getValue();
            }
        }
        for (UUID activeUserId : activeUsersInRange) {
            UserEntity user = userById.get(activeUserId);
            DepartmentAccumulator accumulator = departmentAccumulators.get(departmentBucketKey(user));
            if (accumulator != null) {
                accumulator.activeUsersInRange += 1;
            }
        }

        List<AnalyticsDepartmentInsightResponse> departmentInsights = departmentAccumulators.values().stream()
                .map(DepartmentAccumulator::toResponse)
                .sorted(Comparator
                        .comparingInt(AnalyticsDepartmentInsightResponse::matchParticipationsInRange).reversed()
                        .thenComparingInt(AnalyticsDepartmentInsightResponse::activeUsersInRange).reversed()
                        .thenComparing(AnalyticsDepartmentInsightResponse::departmentDisplayName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        String topDepartmentByParticipation = departmentInsights.stream()
                .filter(item -> item.matchParticipationsInRange() > 0)
                .findFirst()
                .map(AnalyticsDepartmentInsightResponse::departmentDisplayName)
                .orElse("No participation in selected range");

        List<AnalyticsActivityPointResponse> activityTrend = new ArrayList<>();
        for (LocalDate date = trendStartDate; !date.isAfter(today); date = date.plusDays(1)) {
            int activeUsers = activeUsersByDay.getOrDefault(date, Set.of()).size();
            int matchesPlayed = matchesByDay.getOrDefault(date, 0);
            int roomsCreated = roomsCreatedByDay.getOrDefault(date, 0);
            activityTrend.add(new AnalyticsActivityPointResponse(date.toString(), activeUsers, matchesPlayed, roomsCreated));
        }

        int totalMatchesForUsage = matchesInRange;
        List<AnalyticsGameUsageResponse> gameUsage = gameCounters.values().stream()
                .sorted(Comparator.comparingInt(GameCounter::matches).reversed()
                        .thenComparing(GameCounter::gameTypeDisplayName, String.CASE_INSENSITIVE_ORDER))
                .map(counter -> new AnalyticsGameUsageResponse(
                        counter.gameTypeCode(),
                        counter.gameTypeDisplayName(),
                        counter.matches(),
                        totalMatchesForUsage == 0 ? 0.0 : round((counter.matches() * 100.0) / totalMatchesForUsage, 1)
                ))
                .toList();

        List<AnalyticsCategoryCountResponse> categoryCounts = reportCategoryCountsInRange.entrySet().stream()
                .map(entry -> new AnalyticsCategoryCountResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(AnalyticsCategoryCountResponse::count).reversed()
                        .thenComparing(AnalyticsCategoryCountResponse::category, String.CASE_INSENSITIVE_ORDER))
                .toList();

        AnalyticsSummaryResponse summary = new AnalyticsSummaryResponse(
                totalEnabledUsers,
                activeUsersInRange.size(),
                activeUsersToday.size(),
                matchesInRange,
                matchesToday,
                roomsCreatedInRange,
                departments.size(),
                respectAwardedInRange,
                karmaAppliedInRange,
                openReports + inReviewReports,
                suspendedUsers,
                activeUsersInRange.isEmpty() ? 0.0 : round((double) matchesInRange / activeUsersInRange.size(), 2),
                topDepartmentByParticipation
        );

        AnalyticsReputationResponse reputation = new AnalyticsReputationResponse(
                challengesCreatedInRange,
                challengesPending,
                challengesDisputed,
                confirmedChallengesInRange,
                rejectedChallengesInRange,
                respectAwardedInRange,
                karmaAppliedInRange
        );

        AnalyticsModerationResponse moderation = new AnalyticsModerationResponse(
                openReports,
                inReviewReports,
                resolvedReports,
                dismissedReports,
                reportsCreatedInRange,
                suspendedUsers,
                categoryCounts
        );

        AnalyticsFilterResponse filters = new AnalyticsFilterResponse(
                range.queryValue(),
                range.label(),
                scope.departmentFilterValue(),
                scope.selectedDepartment() == null ? null : toSummary(scope.selectedDepartment()),
                scope.unassignedOnly()
        );

        String activityTrendLabel = range == AnalyticsRange.ALL
                ? "Last 30 days activity trend"
                : range.label() + " activity trend";

        return new AdminAnalyticsDashboardResponse(
                filters,
                summary,
                activityTrend,
                activityTrendLabel,
                gameUsage,
                departmentInsights,
                reputation,
                moderation,
                now.toString()
        );
    }

    private static boolean isMatchInScope(List<UUID> participantIds, boolean scopeAll, Set<UUID> scopedUserIds) {
        if (scopeAll) {
            return true;
        }
        for (UUID participantId : participantIds) {
            if (scopedUserIds.contains(participantId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isChallengeInScope(
            UUID obligatedUserId,
            UUID beneficiaryUserId,
            boolean scopeAll,
            Set<UUID> scopedUserIds
    ) {
        if (scopeAll) {
            return true;
        }
        return (obligatedUserId != null && scopedUserIds.contains(obligatedUserId))
                || (beneficiaryUserId != null && scopedUserIds.contains(beneficiaryUserId));
    }

    private static List<UUID> participants(UUID playerOneId, UUID playerTwoId) {
        List<UUID> participants = new ArrayList<>(2);
        if (playerOneId != null) {
            participants.add(playerOneId);
        }
        if (playerTwoId != null && !Objects.equals(playerTwoId, playerOneId)) {
            participants.add(playerTwoId);
        }
        return participants;
    }

    private static String gameTypeCode(RoomEntity room, String fallback) {
        if (room == null || room.getGameType() == null || room.getGameType().getCode() == null) {
            return fallback;
        }
        return room.getGameType().getCode();
    }

    private static String gameTypeDisplayName(RoomEntity room, String fallback) {
        if (room == null || room.getGameType() == null || room.getGameType().getDisplayName() == null) {
            return fallback;
        }
        return room.getGameType().getDisplayName();
    }

    private static Map<String, DepartmentAccumulator> initializeDepartmentAccumulators(
            DepartmentScope scope,
            List<DepartmentEntity> departments
    ) {
        Map<String, DepartmentAccumulator> accumulators = new LinkedHashMap<>();
        if (scope.scopeAll()) {
            for (DepartmentEntity department : departments) {
                accumulators.put(department.getId().toString(), DepartmentAccumulator.forDepartment(department));
            }
            accumulators.put(UNASSIGNED_BUCKET_KEY, DepartmentAccumulator.unassigned());
            return accumulators;
        }

        if (scope.unassignedOnly()) {
            accumulators.put(UNASSIGNED_BUCKET_KEY, DepartmentAccumulator.unassigned());
            return accumulators;
        }

        if (scope.selectedDepartment() != null) {
            accumulators.put(
                    scope.selectedDepartment().getId().toString(),
                    DepartmentAccumulator.forDepartment(scope.selectedDepartment())
            );
        }
        return accumulators;
    }

    private DepartmentScope resolveDepartmentScope(String departmentFilterQuery, List<UserEntity> allUsers) {
        Set<UUID> allUserIds = new HashSet<>();
        for (UserEntity user : allUsers) {
            allUserIds.add(user.getId());
        }

        if (departmentFilterQuery == null || departmentFilterQuery.trim().isEmpty()
                || "ALL".equalsIgnoreCase(departmentFilterQuery.trim())) {
            return new DepartmentScope(true, false, allUserIds, null, "ALL");
        }

        if ("UNASSIGNED".equalsIgnoreCase(departmentFilterQuery.trim())) {
            Set<UUID> unassignedUserIds = new HashSet<>();
            for (UserEntity user : allUsers) {
                if (user.getDepartment() == null) {
                    unassignedUserIds.add(user.getId());
                }
            }
            return new DepartmentScope(false, true, unassignedUserIds, null, "UNASSIGNED");
        }

        UUID departmentId;
        try {
            departmentId = UUID.fromString(departmentFilterQuery.trim());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department filter must be a UUID value.");
        }

        DepartmentEntity selectedDepartment = departmentEntityRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Department not found: " + departmentFilterQuery
                ));

        Set<UUID> departmentUserIds = new HashSet<>();
        for (UserEntity user : allUsers) {
            if (user.getDepartment() != null && departmentId.equals(user.getDepartment().getId())) {
                departmentUserIds.add(user.getId());
            }
        }

        return new DepartmentScope(false, false, departmentUserIds, selectedDepartment, selectedDepartment.getId().toString());
    }

    private static DepartmentSummaryResponse toSummary(DepartmentEntity department) {
        return new DepartmentSummaryResponse(
                department.getId().toString(),
                department.getCode(),
                department.getDisplayName(),
                department.isActive()
        );
    }

    private static String departmentBucketKey(UserEntity user) {
        if (user == null || user.getDepartment() == null || user.getDepartment().getId() == null) {
            return UNASSIGNED_BUCKET_KEY;
        }
        return user.getDepartment().getId().toString();
    }

    private static boolean isInRange(Instant eventTime, Instant rangeStart, Instant now) {
        if (eventTime == null || eventTime.isAfter(now)) {
            return false;
        }
        if (rangeStart == null) {
            return true;
        }
        return !eventTime.isBefore(rangeStart);
    }

    private static Instant resolveEventTime(Instant primary, Instant secondary, Instant fallback) {
        if (primary != null) {
            return primary;
        }
        if (secondary != null) {
            return secondary;
        }
        return fallback;
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private record DepartmentScope(
            boolean scopeAll,
            boolean unassignedOnly,
            Set<UUID> scopedUserIds,
            DepartmentEntity selectedDepartment,
            String departmentFilterValue
    ) {
    }

    private static final class GameCounter {
        private final String gameTypeCode;
        private final String gameTypeDisplayName;
        private int matches;

        private GameCounter(String gameTypeCode, String gameTypeDisplayName) {
            this.gameTypeCode = gameTypeCode;
            this.gameTypeDisplayName = gameTypeDisplayName;
        }

        private String gameTypeCode() {
            return gameTypeCode;
        }

        private String gameTypeDisplayName() {
            return gameTypeDisplayName;
        }

        private int matches() {
            return matches;
        }
    }

    private static final class DepartmentAccumulator {
        private final String departmentId;
        private final String departmentCode;
        private final String departmentDisplayName;
        private final boolean departmentActive;
        private final boolean unassignedBucket;
        private int userCount;
        private int activeUsersInRange;
        private int matchParticipationsInRange;
        private int profileCount;
        private int totalRespect;
        private int totalKarma;

        private DepartmentAccumulator(
                String departmentId,
                String departmentCode,
                String departmentDisplayName,
                boolean departmentActive,
                boolean unassignedBucket
        ) {
            this.departmentId = departmentId;
            this.departmentCode = departmentCode;
            this.departmentDisplayName = departmentDisplayName;
            this.departmentActive = departmentActive;
            this.unassignedBucket = unassignedBucket;
        }

        private static DepartmentAccumulator forDepartment(DepartmentEntity department) {
            return new DepartmentAccumulator(
                    department.getId().toString(),
                    department.getCode(),
                    department.getDisplayName(),
                    department.isActive(),
                    false
            );
        }

        private static DepartmentAccumulator unassigned() {
            return new DepartmentAccumulator(
                    null,
                    UNASSIGNED_BUCKET_CODE,
                    UNASSIGNED_BUCKET_NAME,
                    true,
                    true
            );
        }

        private AnalyticsDepartmentInsightResponse toResponse() {
            double averageRespect = profileCount == 0 ? 0.0 : round((double) totalRespect / profileCount, 1);
            double averageKarma = profileCount == 0 ? 0.0 : round((double) totalKarma / profileCount, 1);
            return new AnalyticsDepartmentInsightResponse(
                    departmentId,
                    departmentCode,
                    departmentDisplayName,
                    departmentActive,
                    unassignedBucket,
                    userCount,
                    activeUsersInRange,
                    matchParticipationsInRange,
                    averageRespect,
                    averageKarma
            );
        }
    }
}
