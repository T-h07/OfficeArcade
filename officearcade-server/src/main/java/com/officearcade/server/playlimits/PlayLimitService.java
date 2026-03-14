package com.officearcade.server.playlimits;

import com.officearcade.server.playlimits.dto.PlayLimitSummaryResponse;
import com.officearcade.server.playlimits.persistence.UserPlayLimitStateEntity;
import com.officearcade.server.playlimits.persistence.UserPlayLimitStateEntityRepository;
import com.officearcade.server.users.persistence.UserEntityRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlayLimitService {

    private static final Set<String> PLAYABLE_GAME_TYPES = Set.of("CONNECT_FOUR", "TRIVIA");

    private final UserPlayLimitStateEntityRepository userPlayLimitStateEntityRepository;
    private final UserEntityRepository userEntityRepository;
    private final PlayLimitPolicyProperties playLimitPolicyProperties;

    public PlayLimitService(
            UserPlayLimitStateEntityRepository userPlayLimitStateEntityRepository,
            UserEntityRepository userEntityRepository,
            PlayLimitPolicyProperties playLimitPolicyProperties
    ) {
        this.userPlayLimitStateEntityRepository = userPlayLimitStateEntityRepository;
        this.userEntityRepository = userEntityRepository;
        this.playLimitPolicyProperties = playLimitPolicyProperties;
    }

    @Transactional
    public PlayLimitSummaryResponse getSummaryForUser(String userIdText) {
        return getSummaryForUser(parseUserId(userIdText));
    }

    @Transactional
    public PlayLimitSummaryResponse getSummaryForUser(UUID userId) {
        Instant now = Instant.now();
        UserPlayLimitStateEntity state = getOrCreateStateForUpdate(userId, now);
        PlayLimitAssessment assessment = refreshAndAssess(state, now);
        userPlayLimitStateEntityRepository.save(state);
        return toSummaryResponse(state, assessment, now);
    }

    @Transactional
    public void assertEligibleForPlayableGame(UUID userId, String gameTypeCode) {
        if (!isPlayableGameType(gameTypeCode)) {
            return;
        }

        Instant now = Instant.now();
        UserPlayLimitStateEntity state = getOrCreateStateForUpdate(userId, now);
        PlayLimitAssessment assessment = refreshAndAssess(state, now);
        userPlayLimitStateEntityRepository.save(state);

        if (!assessment.canPlayNow()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, toBlockedMessage(assessment));
        }
    }

    @Transactional
    public void recordCompletedMatchForUsers(List<UUID> participantUserIds) {
        if (participantUserIds == null || participantUserIds.isEmpty()) {
            return;
        }

        Instant completedAt = Instant.now();
        int dailyLimit = playLimitPolicyProperties.getDailyGameLimit();
        long cooldownMinutes = playLimitPolicyProperties.getCooldownMinutes();
        ZoneId zoneId = playLimitPolicyProperties.zoneId();
        LocalDate today = ZonedDateTime.ofInstant(completedAt, zoneId).toLocalDate();

        Set<UUID> uniqueUserIds = new LinkedHashSet<>(participantUserIds);
        for (UUID userId : uniqueUserIds) {
            UserPlayLimitStateEntity state = getOrCreateStateForUpdate(userId, completedAt);
            refreshForToday(state, today);

            state.setGamesPlayedToday(state.getGamesPlayedToday() + 1);
            state.setLastCompletedGameAt(completedAt);
            if (cooldownMinutes > 0) {
                state.setCooldownUntil(completedAt.plus(Duration.ofMinutes(cooldownMinutes)));
            } else {
                state.setCooldownUntil(null);
            }

            if (state.getGamesPlayedToday() > dailyLimit + 1000) {
                // Guardrail for accidental runaway writes in case of duplicate completion events.
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Play-limit counter drift detected for user: " + userId
                );
            }

            userPlayLimitStateEntityRepository.save(state);
        }
    }

    private PlayLimitSummaryResponse toSummaryResponse(
            UserPlayLimitStateEntity state,
            PlayLimitAssessment assessment,
            Instant now
    ) {
        ZoneId zoneId = playLimitPolicyProperties.zoneId();
        ZonedDateTime nextReset = ZonedDateTime.ofInstant(now, zoneId)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay(zoneId);

        return new PlayLimitSummaryResponse(
                state.getUserId().toString(),
                playLimitPolicyProperties.getDailyGameLimit(),
                state.getGamesPlayedToday(),
                assessment.gamesRemainingToday(),
                assessment.cooldownActive(),
                nullableInstant(state.getCooldownUntil()),
                assessment.cooldownRemainingSeconds(),
                assessment.canPlayNow(),
                assessment.reason().name(),
                state.getGamesPlayedDate().toString(),
                nextReset.toInstant().toString(),
                nullableInstant(state.getLastCompletedGameAt()),
                state.getUpdatedAt().toString()
        );
    }

    private PlayLimitAssessment refreshAndAssess(UserPlayLimitStateEntity state, Instant now) {
        ZoneId zoneId = playLimitPolicyProperties.zoneId();
        LocalDate today = ZonedDateTime.ofInstant(now, zoneId).toLocalDate();
        refreshForToday(state, today);

        int dailyLimit = playLimitPolicyProperties.getDailyGameLimit();
        int gamesRemainingToday = Math.max(dailyLimit - state.getGamesPlayedToday(), 0);

        if (state.getGamesPlayedToday() >= dailyLimit) {
            return new PlayLimitAssessment(
                    false,
                    PlayLimitEligibilityReason.DAILY_LIMIT_REACHED,
                    gamesRemainingToday,
                    false,
                    0L
            );
        }

        Instant cooldownUntil = state.getCooldownUntil();
        if (cooldownUntil != null && now.isBefore(cooldownUntil)) {
            long remainingSeconds = Math.max(Duration.between(now, cooldownUntil).toSeconds(), 0L);
            return new PlayLimitAssessment(
                    false,
                    PlayLimitEligibilityReason.COOLDOWN_ACTIVE,
                    gamesRemainingToday,
                    true,
                    remainingSeconds
            );
        }

        return new PlayLimitAssessment(
                true,
                PlayLimitEligibilityReason.ELIGIBLE,
                gamesRemainingToday,
                false,
                0L
        );
    }

    private static String toBlockedMessage(PlayLimitAssessment assessment) {
        if (assessment.reason() == PlayLimitEligibilityReason.DAILY_LIMIT_REACHED) {
            return "Daily play limit reached. You can play again after the daily reset.";
        }

        if (assessment.reason() == PlayLimitEligibilityReason.COOLDOWN_ACTIVE) {
            long minutesRemaining = Math.max(1L, (assessment.cooldownRemainingSeconds() + 59L) / 60L);
            return "You are on cooldown. Try again in " + minutesRemaining + " minute(s).";
        }

        return "You are not eligible to play right now.";
    }

    private UserPlayLimitStateEntity getOrCreateStateForUpdate(UUID userId, Instant now) {
        return userPlayLimitStateEntityRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> {
                    if (!userEntityRepository.existsById(userId)) {
                        throw new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated user is no longer available."
                        );
                    }

                    UserPlayLimitStateEntity created = new UserPlayLimitStateEntity();
                    created.setUserId(userId);
                    created.setGamesPlayedDate(ZonedDateTime.ofInstant(now, playLimitPolicyProperties.zoneId()).toLocalDate());
                    created.setGamesPlayedToday(0);
                    created.setCooldownUntil(null);
                    created.setLastCompletedGameAt(null);
                    return userPlayLimitStateEntityRepository.save(created);
                });
    }

    private static void refreshForToday(UserPlayLimitStateEntity state, LocalDate today) {
        if (state.getGamesPlayedDate() == null || !state.getGamesPlayedDate().isEqual(today)) {
            state.setGamesPlayedDate(today);
            state.setGamesPlayedToday(0);
            state.setCooldownUntil(null);
        }
    }

    private static boolean isPlayableGameType(String gameTypeCode) {
        if (gameTypeCode == null) {
            return false;
        }
        return PLAYABLE_GAME_TYPES.contains(gameTypeCode.trim().toUpperCase());
    }

    private static String nullableInstant(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private static UUID parseUserId(String userIdText) {
        try {
            return UUID.fromString(userIdText);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authenticated user context.");
        }
    }

    private record PlayLimitAssessment(
            boolean canPlayNow,
            PlayLimitEligibilityReason reason,
            int gamesRemainingToday,
            boolean cooldownActive,
            long cooldownRemainingSeconds
    ) {
    }
}
