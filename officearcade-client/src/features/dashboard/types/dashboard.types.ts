import type { AppRole } from "../../auth/auth.types";
import type { DepartmentSummary } from "../../departments/types/departments.types";

export type DashboardGameType = {
  code: string;
  displayName: string;
};

export type DashboardChallengeSummary = {
  id: string;
  challengeTypeCode: string;
  challengeTypeDisplayName: string;
  status: "PENDING" | "DISPUTED" | "COMPLETED_CONFIRMED" | "REJECTED" | "CANCELLED" | "EXPIRED";
  myRole: "OBLIGATED" | "BENEFICIARY";
  counterpartyDisplayName: string;
  createdAt: string;
  resolvedAt: string | null;
};

export type DashboardEquippedCosmetic = {
  cosmeticItemId: string;
  code: string;
  displayName: string;
  category: "HAT" | "GLASSES" | "OUTFIT" | "PROFILE_FRAME" | "BADGE" | "ACCESSORY";
  rarity: "COMMON" | "RARE" | "EPIC";
  previewAssetKey: string;
  equippedAt: string;
};

export type EmployeeDashboardResponse = {
  userId: string;
  displayName: string;
  email: string;
  role: AppRole;
  accountEnabled: boolean;
  department: DepartmentSummary | null;
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
  ownedCosmeticCount: number;
  equippedCosmeticCount: number;
  equippedCosmetics: DashboardEquippedCosmetic[];
  pendingChallengeCount: number;
  resolvedChallengeCount: number;
  recentChallenges: DashboardChallengeSummary[];
  profileUpdatedAt: string;
  generatedAt: string;
};
