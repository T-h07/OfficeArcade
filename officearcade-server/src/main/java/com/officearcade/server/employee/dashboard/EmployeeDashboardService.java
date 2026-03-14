package com.officearcade.server.employee.dashboard;

import com.officearcade.server.challenges.PostMatchChallengeService;
import com.officearcade.server.challenges.dto.DashboardChallengeSummaryResponse;
import com.officearcade.server.catalog.persistence.GameTypeEntityRepository;
import com.officearcade.server.employee.dashboard.dto.DashboardEquippedCosmeticResponse;
import com.officearcade.server.employee.dashboard.dto.DashboardGameTypeResponse;
import com.officearcade.server.employee.dashboard.dto.EmployeeDashboardResponse;
import com.officearcade.server.profiles.persistence.PlayerProfileEntity;
import com.officearcade.server.profiles.persistence.PlayerProfileEntityRepository;
import com.officearcade.server.store.persistence.UserEquippedCosmeticEntityRepository;
import com.officearcade.server.store.persistence.UserOwnedCosmeticEntityRepository;
import com.officearcade.server.users.UserAccount;
import com.officearcade.server.users.UserAccountService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmployeeDashboardService {

    private static final int XP_PER_LEVEL = 250;

    private final UserAccountService userAccountService;
    private final PlayerProfileEntityRepository playerProfileEntityRepository;
    private final GameTypeEntityRepository gameTypeEntityRepository;
    private final PostMatchChallengeService postMatchChallengeService;
    private final UserOwnedCosmeticEntityRepository userOwnedCosmeticEntityRepository;
    private final UserEquippedCosmeticEntityRepository userEquippedCosmeticEntityRepository;

    public EmployeeDashboardService(
            UserAccountService userAccountService,
            PlayerProfileEntityRepository playerProfileEntityRepository,
            GameTypeEntityRepository gameTypeEntityRepository,
            PostMatchChallengeService postMatchChallengeService,
            UserOwnedCosmeticEntityRepository userOwnedCosmeticEntityRepository,
            UserEquippedCosmeticEntityRepository userEquippedCosmeticEntityRepository
    ) {
        this.userAccountService = userAccountService;
        this.playerProfileEntityRepository = playerProfileEntityRepository;
        this.gameTypeEntityRepository = gameTypeEntityRepository;
        this.postMatchChallengeService = postMatchChallengeService;
        this.userOwnedCosmeticEntityRepository = userOwnedCosmeticEntityRepository;
        this.userEquippedCosmeticEntityRepository = userEquippedCosmeticEntityRepository;
    }

    @Transactional(readOnly = true)
    public EmployeeDashboardResponse getDashboardForUser(String userIdText) {
        UUID userId = parseUserId(userIdText);

        UserAccount user = userAccountService.findById(userId.toString())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated."));

        PlayerProfileEntity profile = playerProfileEntityRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player profile is not initialized for user: " + user.id()
                ));

        int level = Math.max(profile.getLevel(), 1);
        int xp = Math.max(profile.getXp(), 0);
        int gamesPlayed = Math.max(profile.getGamesPlayed(), 0);
        int wins = Math.max(profile.getWins(), 0);
        int losses = Math.max(profile.getLosses(), 0);
        int totalMatches = wins + losses;

        int xpForNextLevel = level * XP_PER_LEVEL;
        int xpToNextLevel = Math.max(0, xpForNextLevel - xp);

        double winRatePercent = gamesPlayed == 0
                ? 0.0
                : roundToSingleDecimal((wins * 100.0) / gamesPlayed);
        double xpProgressPercent = xpForNextLevel == 0
                ? 0.0
                : roundToSingleDecimal(Math.min(100.0, (xp * 100.0) / xpForNextLevel));

        List<DashboardGameTypeResponse> enabledGameTypes = gameTypeEntityRepository
                .findAllByEnabledTrueOrderByDisplayNameAsc()
                .stream()
                .map(gameType -> new DashboardGameTypeResponse(gameType.getCode(), gameType.getDisplayName()))
                .toList();

        int pendingChallengeCount = postMatchChallengeService.getPendingChallengeCount(user.id());
        int resolvedChallengeCount = postMatchChallengeService.getResolvedChallengeCount(user.id());
        List<DashboardChallengeSummaryResponse> recentChallenges = postMatchChallengeService
                .getRecentChallengeSummaries(user.id(), 3);
        int ownedCosmeticCount = (int) userOwnedCosmeticEntityRepository.countByUser_Id(userId);
        List<DashboardEquippedCosmeticResponse> equippedCosmetics = userEquippedCosmeticEntityRepository
                .findAllByUser_IdOrderByEquippedAtDesc(userId)
                .stream()
                .map(entry -> new DashboardEquippedCosmeticResponse(
                        entry.getCosmeticItem().getId().toString(),
                        entry.getCosmeticItem().getCode(),
                        entry.getCosmeticItem().getDisplayName(),
                        entry.getCategory().name(),
                        entry.getCosmeticItem().getRarity().name(),
                        entry.getCosmeticItem().getPreviewAssetKey(),
                        entry.getEquippedAt().toString()
                ))
                .toList();

        return new EmployeeDashboardResponse(
                user.id(),
                user.displayName(),
                user.email(),
                user.role().name(),
                user.enabled(),
                level,
                xp,
                Math.max(profile.getRespectPoints(), 0),
                Math.max(profile.getKarmaPoints(), 0),
                gamesPlayed,
                wins,
                losses,
                totalMatches,
                winRatePercent,
                xpForNextLevel,
                xpToNextLevel,
                xpProgressPercent,
                enabledGameTypes.size(),
                enabledGameTypes,
                ownedCosmeticCount,
                equippedCosmetics.size(),
                equippedCosmetics,
                pendingChallengeCount,
                resolvedChallengeCount,
                recentChallenges,
                profile.getUpdatedAt().toString(),
                Instant.now().toString()
        );
    }

    private static UUID parseUserId(String userIdText) {
        try {
            return UUID.fromString(userIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        }
    }

    private static double roundToSingleDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
