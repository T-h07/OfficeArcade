import type { AppRole } from "../../auth/auth.types";
import type { DepartmentSummary } from "../../departments/types/departments.types";

export type LeaderboardType =
  | "WINS"
  | "WIN_RATE"
  | "LEVEL"
  | "RESPECT"
  | "KARMA"
  | "GAMES_PLAYED";

export type LeaderboardTypeOption = {
  type: LeaderboardType;
  title: string;
  metricLabel: string;
  rankingDirection: "ASC" | "DESC";
  minimumCompletedMatches: number;
  description: string;
};

export type LeaderboardTypeListResponse = {
  types: LeaderboardTypeOption[];
  generatedAt: string;
};

export type LeaderboardEntry = {
  rank: number;
  userId: string;
  displayName: string;
  role: AppRole;
  department: DepartmentSummary | null;
  level: number;
  xp: number;
  gamesPlayed: number;
  wins: number;
  losses: number;
  respectPoints: number;
  karmaPoints: number;
  winRatePercent: number;
  primaryMetricValue: number;
  primaryMetricDisplay: string;
  profileFrameAssetKey: string | null;
  badgeAssetKey: string | null;
  currentUser: boolean;
};

export type LeaderboardDepartmentFilter = "ALL" | "UNASSIGNED" | string;

export type LeaderboardResponse = {
  type: LeaderboardType;
  title: string;
  metricLabel: string;
  rankingDirection: "ASC" | "DESC";
  minimumCompletedMatches: number;
  limit: number;
  totalEligibleEntries: number;
  entries: LeaderboardEntry[];
  currentUserEntry: LeaderboardEntry | null;
  currentUserEligible: boolean;
  currentUserNote: string | null;
  generatedAt: string;
};
