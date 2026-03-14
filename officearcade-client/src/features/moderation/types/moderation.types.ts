export type ModerationReportCategory =
  | "UNSPORTSMANLIKE_BEHAVIOR"
  | "CHALLENGE_DISPUTE"
  | "HARASSMENT_OR_INAPPROPRIATE_BEHAVIOR"
  | "ABUSE_OF_SYSTEM"
  | "OTHER";

export type ModerationReportStatus = "OPEN" | "IN_REVIEW" | "RESOLVED" | "DISMISSED";

export type ModerationReportActionType = "NOTE_ONLY" | "SUSPEND_USER" | "UNSUSPEND_USER";

export type ChallengeDisputeResolutionType = "CONFIRM_COMPLETED" | "REJECT_NOT_FULFILLED" | "CANCEL_WITHOUT_POINTS";

export type CreateModerationReportRequest = {
  reportedUserId: string;
  category: ModerationReportCategory;
  note?: string;
  sourceRoomId?: string;
  sourceGameSessionId?: string;
  sourceChallengeId?: string;
};

export type ModerationReport = {
  id: string;
  reporterUserId: string;
  reporterDisplayName: string;
  reportedUserId: string;
  reportedDisplayName: string;
  category: ModerationReportCategory;
  note: string | null;
  status: ModerationReportStatus;
  sourceRoomId: string | null;
  sourceGameSessionId: string | null;
  sourceChallengeId: string | null;
  reviewedByAdminId: string | null;
  reviewedByAdminDisplayName: string | null;
  resolutionNote: string | null;
  createdAt: string;
  updatedAt: string;
};

export type ModerationReportListResponse = {
  total: number;
  reports: ModerationReport[];
};

export type ModerationUserStateResponse = {
  userId: string;
  displayName: string;
  suspended: boolean;
  suspendedAt: string | null;
  suspensionNote: string | null;
};

export type ChallengePolicySetting = {
  code: string;
  displayName: string;
  description: string;
  respectRewardPoints: number;
  karmaPenaltyPoints: number;
  enabled: boolean;
  updatedAt: string;
};

export type ChallengePolicyListResponse = {
  total: number;
  policies: ChallengePolicySetting[];
};

export type ChallengePolicyUpdateRequest = {
  policies: Array<{
    code: string;
    enabled: boolean;
  }>;
};

export type AdminChallengeReview = {
  challengeId: string;
  sourceRoomId: string;
  sourceGameSessionId: string;
  challengeTypeCode: string;
  challengeTypeDisplayName: string;
  status: string;
  obligatedUserId: string;
  obligatedDisplayName: string;
  beneficiaryUserId: string;
  beneficiaryDisplayName: string;
  disputeNote: string | null;
  disputedAt: string | null;
  createdAt: string;
};

export type AdminChallengeReviewListResponse = {
  total: number;
  challenges: AdminChallengeReview[];
};

export type ModerationAuditEntry = {
  id: string;
  adminUserId: string;
  adminDisplayName: string;
  targetUserId: string | null;
  targetDisplayName: string | null;
  actionType: string;
  reportId: string | null;
  challengeId: string | null;
  note: string | null;
  createdAt: string;
};

export type ModerationAuditListResponse = {
  total: number;
  entries: ModerationAuditEntry[];
};
