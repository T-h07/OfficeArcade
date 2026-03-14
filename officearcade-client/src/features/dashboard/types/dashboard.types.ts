import type { AppRole } from "../../auth/auth.types";

export type DashboardGameType = {
  code: string;
  displayName: string;
};

export type DashboardChallengeSummary = {
  id: string;
  challengeTypeCode: string;
  challengeTypeDisplayName: string;
  status: "PENDING" | "COMPLETED_CONFIRMED" | "REJECTED" | "EXPIRED";
  myRole: "OBLIGATED" | "BENEFICIARY";
  counterpartyDisplayName: string;
  createdAt: string;
  resolvedAt: string | null;
};

export type EmployeeDashboardResponse = {
  userId: string;
  displayName: string;
  email: string;
  role: AppRole;
  accountEnabled: boolean;
  level: number;
  xp: number;
  respectPoints: number;
  karmaPoints: number;
  gamesPlayed: number;
  wins: number;
  losses: number;
  totalMatches: number;
  winRatePercent: number;
  xpForNextLevel: number;
  xpToNextLevel: number;
  xpProgressPercent: number;
  enabledGameTypeCount: number;
  enabledGameTypes: DashboardGameType[];
  pendingChallengeCount: number;
  resolvedChallengeCount: number;
  recentChallenges: DashboardChallengeSummary[];
  profileUpdatedAt: string;
  generatedAt: string;
};
