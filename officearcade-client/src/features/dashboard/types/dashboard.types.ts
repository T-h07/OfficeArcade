import type { AppRole } from "../../auth/auth.types";

export type DashboardGameType = {
  code: string;
  displayName: string;
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
  profileUpdatedAt: string;
  generatedAt: string;
};
