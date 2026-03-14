export type PlayLimitEligibilityReason = "ELIGIBLE" | "COOLDOWN_ACTIVE" | "DAILY_LIMIT_REACHED";

export type PlayLimitSummary = {
  userId: string;
  dailyGameLimit: number;
  gamesPlayedToday: number;
  gamesRemainingToday: number;
  cooldownActive: boolean;
  cooldownUntil: string | null;
  cooldownRemainingSeconds: number;
  canPlayNow: boolean;
  eligibilityReason: PlayLimitEligibilityReason;
  gamesPlayedDate: string;
  nextDailyResetAt: string;
  lastCompletedGameAt: string | null;
  updatedAt: string;
};
