import type { DepartmentSummary } from "../../departments/types/departments.types";

export type AnalyticsRange = "today" | "7d" | "30d" | "all";
export type AnalyticsDepartmentFilter = "ALL" | "UNASSIGNED" | string;

export type AnalyticsFilterInfo = {
  range: AnalyticsRange;
  rangeLabel: string;
  departmentFilter: AnalyticsDepartmentFilter;
  selectedDepartment: DepartmentSummary | null;
  unassignedOnly: boolean;
};

export type AnalyticsSummary = {
  totalEnabledUsers: number;
  activeUsersInRange: number;
  activeUsersToday: number;
  matchesInRange: number;
  matchesToday: number;
  roomsCreatedInRange: number;
  totalDepartments: number;
  respectAwardedInRange: number;
  karmaAppliedInRange: number;
  openModerationReports: number;
  suspendedUsers: number;
  averageMatchesPerActiveUser: number;
  topDepartmentByParticipation: string;
};

export type AnalyticsActivityPoint = {
  date: string;
  activeUsers: number;
  matchesPlayed: number;
  roomsCreated: number;
};

export type AnalyticsGameUsage = {
  gameTypeCode: string;
  gameTypeDisplayName: string;
  matchesPlayed: number;
  percentOfMatches: number;
};

export type AnalyticsDepartmentInsight = {
  departmentId: string | null;
  departmentCode: string;
  departmentDisplayName: string;
  departmentActive: boolean;
  unassignedBucket: boolean;
  userCount: number;
  activeUsersInRange: number;
  matchParticipationsInRange: number;
  averageRespect: number;
  averageKarma: number;
};

export type AnalyticsReputation = {
  challengesCreatedInRange: number;
  challengesPending: number;
  challengesDisputed: number;
  confirmedChallengesInRange: number;
  rejectedChallengesInRange: number;
  respectAwardedInRange: number;
  karmaAppliedInRange: number;
};

export type AnalyticsCategoryCount = {
  category: string;
  count: number;
};

export type AnalyticsModeration = {
  openReports: number;
  inReviewReports: number;
  resolvedReports: number;
  dismissedReports: number;
  reportsCreatedInRange: number;
  suspendedUsers: number;
  reportCategoriesInRange: AnalyticsCategoryCount[];
};

export type AdminAnalyticsDashboardResponse = {
  filters: AnalyticsFilterInfo;
  summary: AnalyticsSummary;
  activityTrend: AnalyticsActivityPoint[];
  activityTrendLabel: string;
  gameUsage: AnalyticsGameUsage[];
  departments: AnalyticsDepartmentInsight[];
  reputation: AnalyticsReputation;
  moderation: AnalyticsModeration;
  generatedAt: string;
};
