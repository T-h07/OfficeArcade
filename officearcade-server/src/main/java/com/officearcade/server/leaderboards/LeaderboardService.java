package com.officearcade.server.leaderboards;

import com.officearcade.server.departments.dto.DepartmentSummaryResponse;
import com.officearcade.server.departments.persistence.DepartmentEntity;
import com.officearcade.server.departments.persistence.DepartmentEntityRepository;
import com.officearcade.server.leaderboards.dto.LeaderboardEntryResponse;
import com.officearcade.server.leaderboards.dto.LeaderboardResponse;
import com.officearcade.server.leaderboards.dto.LeaderboardTypeListResponse;
import com.officearcade.server.leaderboards.dto.LeaderboardTypeOptionResponse;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.store.CosmeticCategory;
import com.officearcade.server.store.persistence.UserEquippedCosmeticEntity;
import com.officearcade.server.store.persistence.UserEquippedCosmeticEntityRepository;
import com.officearcade.server.users.persistence.UserEntity;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeaderboardService {

    private static final int DEFAULT_LIMIT = 25;
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;

    private final UserEntityRepository userEntityRepository;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final UserEquippedCosmeticEntityRepository userEquippedCosmeticEntityRepository;
    private final DepartmentEntityRepository departmentEntityRepository;

    public LeaderboardService(
            UserEntityRepository userEntityRepository,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            UserEquippedCosmeticEntityRepository userEquippedCosmeticEntityRepository,
            DepartmentEntityRepository departmentEntityRepository
    ) {
        this.userEntityRepository = userEntityRepository;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.userEquippedCosmeticEntityRepository = userEquippedCosmeticEntityRepository;
        this.departmentEntityRepository = departmentEntityRepository;
    }

    @Transactional(readOnly = true)
    public LeaderboardTypeListResponse getAvailableTypes() {
        List<LeaderboardTypeOptionResponse> types = EnumSet.allOf(LeaderboardType.class)
                .stream()
                .map(type -> new LeaderboardTypeOptionResponse(
                        type.name(),
                        type.title(),
                        type.metricLabel(),
                        type.rankingDirection(),
                        type.minimumCompletedMatches(),
                        type.description()
                ))
                .toList();

        return new LeaderboardTypeListResponse(types, Instant.now().toString());
    }

    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(
            String currentUserIdText,
            LeaderboardType type,
            Integer requestedLimit,
            String departmentFilter
    ) {
        UUID currentUserId = parseUserId(currentUserIdText);
        int effectiveLimit = normalizeLimit(requestedLimit);
        DepartmentFilter resolvedDepartmentFilter = resolveDepartmentFilter(departmentFilter);

        List<RankedCandidate> rankedCandidates = rankCandidates(type, resolvedDepartmentFilter);
        List<LeaderboardEntryResponse> entries = rankedCandidates.stream()
                .limit(effectiveLimit)
                .map(candidate -> toResponse(candidate, type, currentUserId))
                .toList();

        Optional<LeaderboardEntryResponse> currentUserEntry = rankedCandidates.stream()
                .filter(candidate -> candidate.candidate().userId().equals(currentUserId))
                .findFirst()
                .map(candidate -> toResponse(candidate, type, currentUserId));

        boolean currentUserEligible = currentUserEntry.isPresent();
        String currentUserNote = currentUserEligible
                ? null
                : resolveIneligibleNote(type);

        return new LeaderboardResponse(
                type.name(),
                type.title(),
                type.metricLabel(),
                type.rankingDirection(),
                type.minimumCompletedMatches(),
                effectiveLimit,
                rankedCandidates.size(),
                entries,
                currentUserEntry.orElse(null),
                currentUserEligible,
                currentUserNote,
                Instant.now().toString()
        );
    }

    @Transactional(readOnly = true)
    public LeaderboardEntryResponse getCurrentUserRank(String currentUserIdText, LeaderboardType type) {
        UUID currentUserId = parseUserId(currentUserIdText);

        return rankCandidates(type, null).stream()
                .filter(candidate -> candidate.candidate().userId().equals(currentUserId))
                .findFirst()
                .map(candidate -> toResponse(candidate, type, currentUserId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, resolveIneligibleNote(type)));
    }

    private List<RankedCandidate> rankCandidates(LeaderboardType type, DepartmentFilter departmentFilter) {
        List<LeaderboardCandidate> candidates = buildCandidates(departmentFilter).stream()
                .filter(candidate -> isEligible(type, candidate))
                .toList();

        Comparator<LeaderboardCandidate> comparator = comparatorFor(type);
        List<LeaderboardCandidate> sorted = candidates.stream()
                .sorted(comparator)
                .toList();

        List<RankedCandidate> ranked = new ArrayList<>(sorted.size());
        LeaderboardCandidate previous = null;
        int currentRank = 0;

        for (LeaderboardCandidate candidate : sorted) {
            if (previous == null || !sameRankGroup(type, previous, candidate)) {
                currentRank += 1;
            }
            ranked.add(new RankedCandidate(currentRank, candidate));
            previous = candidate;
        }

        return ranked;
    }

    private List<LeaderboardCandidate> buildCandidates(DepartmentFilter departmentFilter) {
        List<UserEntity> enabledUsers = userEntityRepository.findAllByEnabledTrueOrderByCreatedAtAscIdAsc();
        if (enabledUsers.isEmpty()) {
            return List.of();
        }

        if (departmentFilter != null) {
            if (departmentFilter.unassignedOnly()) {
                enabledUsers = enabledUsers.stream()
                        .filter(user -> user.getDepartment() == null)
                        .toList();
            } else {
                UUID departmentId = departmentFilter.departmentId();
                enabledUsers = enabledUsers.stream()
                        .filter(user -> user.getDepartment() != null && user.getDepartment().getId().equals(departmentId))
                        .toList();
            }
        }

        if (enabledUsers.isEmpty()) {
            return List.of();
        }

        Map<UUID, PlayerProfileEntity> profileByUserId = new HashMap<>();
        for (PlayerProfileEntity profile : playerProfileEntityRepository.findAll()) {
            profileByUserId.put(profile.getUserId(), profile);
        }

        List<UUID> userIds = enabledUsers.stream().map(UserEntity::getId).toList();
        Map<UUID, CosmeticMarkers> cosmeticMarkersByUserId = loadCosmeticMarkersByUserId(userIds);

        List<LeaderboardCandidate> candidates = new ArrayList<>(enabledUsers.size());
        for (UserEntity user : enabledUsers) {
            PlayerProfileEntity profile = profileByUserId.get(user.getId());
            if (profile == null) {
                continue;
            }

            int level = Math.max(profile.getLevel(), 1);
            int xp = Math.max(profile.getXp(), 0);
            int gamesPlayed = Math.max(profile.getGamesPlayed(), 0);
            int wins = Math.max(profile.getWins(), 0);
            int losses = Math.max(profile.getLosses(), 0);
            int totalMatches = Math.max(0, wins + losses);
            int respectPoints = Math.max(profile.getRespectPoints(), 0);
            int karmaPoints = Math.max(profile.getKarmaPoints(), 0);

            double winRatePercent = totalMatches == 0
                    ? 0.0
                    : round((wins * 100.0) / totalMatches, 1);
            double winRateSortValue = totalMatches == 0
                    ? 0.0
                    : round((wins * 100.0) / totalMatches, 4);

            CosmeticMarkers markers = cosmeticMarkersByUserId.getOrDefault(user.getId(), CosmeticMarkers.EMPTY);
            String departmentId = null;
            String departmentCode = null;
            String departmentDisplayName = null;
            Boolean departmentActive = null;
            if (user.getDepartment() != null) {
                departmentId = user.getDepartment().getId().toString();
                departmentCode = user.getDepartment().getCode();
                departmentDisplayName = user.getDepartment().getDisplayName();
                departmentActive = user.getDepartment().isActive();
            }

            candidates.add(new LeaderboardCandidate(
                    user.getId(),
                    user.getDisplayName(),
                    user.getRole().name(),
                    departmentId,
                    departmentCode,
                    departmentDisplayName,
                    departmentActive,
                    user.getCreatedAt(),
                    level,
                    xp,
                    gamesPlayed,
                    wins,
                    losses,
                    totalMatches,
                    respectPoints,
                    karmaPoints,
                    winRatePercent,
                    winRateSortValue,
                    markers.profileFrameAssetKey(),
                    markers.badgeAssetKey()
            ));
        }

        return candidates;
    }

    private Map<UUID, CosmeticMarkers> loadCosmeticMarkersByUserId(Collection<UUID> userIds) {
        Map<UUID, CosmeticMarkers> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }

        for (UserEquippedCosmeticEntity entry : userEquippedCosmeticEntityRepository
                .findAllByUser_IdInOrderByUser_IdAscEquippedAtDesc(userIds)) {
            if (!entry.getCosmeticItem().isEnabled()) {
                continue;
            }

            UUID userId = entry.getUser().getId();
            CosmeticMarkers current = result.getOrDefault(userId, CosmeticMarkers.EMPTY);

            String frame = current.profileFrameAssetKey();
            String badge = current.badgeAssetKey();

            if (entry.getCategory() == CosmeticCategory.PROFILE_FRAME && frame == null) {
                frame = entry.getCosmeticItem().getPreviewAssetKey();
            } else if (entry.getCategory() == CosmeticCategory.BADGE && badge == null) {
                badge = entry.getCosmeticItem().getPreviewAssetKey();
            }

            result.put(userId, new CosmeticMarkers(frame, badge));
        }

        return result;
    }

    private static boolean isEligible(LeaderboardType type, LeaderboardCandidate candidate) {
        if (type == LeaderboardType.WIN_RATE) {
            return candidate.totalMatches() >= type.minimumCompletedMatches();
        }
        return true;
    }

    private static boolean sameRankGroup(LeaderboardType type, LeaderboardCandidate left, LeaderboardCandidate right) {
        return switch (type) {
            case WINS -> left.wins() == right.wins();
            case WIN_RATE -> Double.compare(left.winRateSortValue(), right.winRateSortValue()) == 0;
            case LEVEL -> left.level() == right.level() && left.xp() == right.xp();
            case RESPECT -> left.respectPoints() == right.respectPoints();
            case KARMA -> left.karmaPoints() == right.karmaPoints();
            case GAMES_PLAYED -> left.gamesPlayed() == right.gamesPlayed();
        };
    }

    private static Comparator<LeaderboardCandidate> comparatorFor(LeaderboardType type) {
        Comparator<LeaderboardCandidate> stable = Comparator
                .comparing((LeaderboardCandidate candidate) -> candidate.displayName().toLowerCase(Locale.ROOT))
                .thenComparing(LeaderboardCandidate::createdAt)
                .thenComparing(candidate -> candidate.userId().toString());

        return switch (type) {
            case WINS -> Comparator
                    .comparingInt(LeaderboardCandidate::wins).reversed()
                    .thenComparingInt(LeaderboardCandidate::losses)
                    .thenComparingInt(LeaderboardCandidate::gamesPlayed).reversed()
                    .thenComparing(stable);
            case WIN_RATE -> Comparator
                    .comparingDouble(LeaderboardCandidate::winRateSortValue).reversed()
                    .thenComparingInt(LeaderboardCandidate::wins).reversed()
                    .thenComparingInt(LeaderboardCandidate::totalMatches).reversed()
                    .thenComparing(stable);
            case LEVEL -> Comparator
                    .comparingInt(LeaderboardCandidate::level).reversed()
                    .thenComparingInt(LeaderboardCandidate::xp).reversed()
                    .thenComparingInt(LeaderboardCandidate::wins).reversed()
                    .thenComparing(stable);
            case RESPECT -> Comparator
                    .comparingInt(LeaderboardCandidate::respectPoints).reversed()
                    .thenComparingInt(LeaderboardCandidate::wins).reversed()
                    .thenComparing(stable);
            case KARMA -> Comparator
                    .comparingInt(LeaderboardCandidate::karmaPoints)
                    .thenComparingInt(LeaderboardCandidate::respectPoints).reversed()
                    .thenComparing(stable);
            case GAMES_PLAYED -> Comparator
                    .comparingInt(LeaderboardCandidate::gamesPlayed).reversed()
                    .thenComparingInt(LeaderboardCandidate::wins).reversed()
                    .thenComparing(stable);
        };
    }

    private static LeaderboardEntryResponse toResponse(
            RankedCandidate rankedCandidate,
            LeaderboardType type,
            UUID currentUserId
    ) {
        LeaderboardCandidate candidate = rankedCandidate.candidate();

        double metricValue = switch (type) {
            case WINS -> candidate.wins();
            case WIN_RATE -> candidate.winRatePercent();
            case LEVEL -> candidate.level();
            case RESPECT -> candidate.respectPoints();
            case KARMA -> candidate.karmaPoints();
            case GAMES_PLAYED -> candidate.gamesPlayed();
        };

        String metricDisplay = switch (type) {
            case WINS -> String.valueOf(candidate.wins());
            case WIN_RATE -> String.format(Locale.ROOT, "%.1f%%", candidate.winRatePercent());
            case LEVEL -> "Lv " + candidate.level();
            case RESPECT -> String.valueOf(candidate.respectPoints());
            case KARMA -> String.valueOf(candidate.karmaPoints());
            case GAMES_PLAYED -> String.valueOf(candidate.gamesPlayed());
        };

        return new LeaderboardEntryResponse(
                rankedCandidate.rank(),
                candidate.userId().toString(),
                candidate.displayName(),
                candidate.role(),
                toDepartmentSummary(candidate),
                candidate.level(),
                candidate.xp(),
                candidate.gamesPlayed(),
                candidate.wins(),
                candidate.losses(),
                candidate.respectPoints(),
                candidate.karmaPoints(),
                candidate.winRatePercent(),
                metricValue,
                metricDisplay,
                candidate.profileFrameAssetKey(),
                candidate.badgeAssetKey(),
                candidate.userId().equals(currentUserId)
        );
    }

    private static int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, requestedLimit));
    }

    private static String resolveIneligibleNote(LeaderboardType type) {
        if (type == LeaderboardType.WIN_RATE) {
            return "Current user is not eligible for Win Rate ranking (minimum completed matches required: "
                    + type.minimumCompletedMatches() + ").";
        }
        return "Current user is not eligible for this leaderboard.";
    }

    private static UUID parseUserId(String userIdText) {
        try {
            return UUID.fromString(userIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        }
    }

    private static double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private DepartmentFilter resolveDepartmentFilter(String departmentFilter) {
        if (departmentFilter == null) {
            return null;
        }

        String normalized = departmentFilter.trim();
        if (normalized.isEmpty() || "ALL".equalsIgnoreCase(normalized)) {
            return null;
        }
        if ("UNASSIGNED".equalsIgnoreCase(normalized)) {
            return DepartmentFilter.unassigned();
        }

        UUID departmentId;
        try {
            departmentId = UUID.fromString(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Department filter must be a UUID.");
        }

        DepartmentEntity department = departmentEntityRepository.findById(departmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Department filter not found: " + departmentFilter
                ));
        return DepartmentFilter.forDepartment(department.getId());
    }

    private static DepartmentSummaryResponse toDepartmentSummary(LeaderboardCandidate candidate) {
        if (candidate.departmentId() == null) {
            return null;
        }
        return new DepartmentSummaryResponse(
                candidate.departmentId(),
                candidate.departmentCode(),
                candidate.departmentDisplayName(),
                Boolean.TRUE.equals(candidate.departmentActive())
        );
    }

    private record RankedCandidate(int rank, LeaderboardCandidate candidate) {
    }

    private record CosmeticMarkers(String profileFrameAssetKey, String badgeAssetKey) {
        private static final CosmeticMarkers EMPTY = new CosmeticMarkers(null, null);
    }

    private record LeaderboardCandidate(
            UUID userId,
            String displayName,
            String role,
            String departmentId,
            String departmentCode,
            String departmentDisplayName,
            Boolean departmentActive,
            Instant createdAt,
            int level,
            int xp,
            int gamesPlayed,
            int wins,
            int losses,
            int totalMatches,
            int respectPoints,
            int karmaPoints,
            double winRatePercent,
            double winRateSortValue,
            String profileFrameAssetKey,
            String badgeAssetKey
    ) {
    }

    private record DepartmentFilter(UUID departmentId, boolean unassignedOnly) {
        private static DepartmentFilter forDepartment(UUID departmentId) {
            return new DepartmentFilter(departmentId, false);
        }

        private static DepartmentFilter unassigned() {
            return new DepartmentFilter(null, true);
        }
    }
}
