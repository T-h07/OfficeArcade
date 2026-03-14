export type ChallengeStatus =
  | "PENDING"
  | "DISPUTED"
  | "COMPLETED_CONFIRMED"
  | "REJECTED"
  | "CANCELLED"
  | "EXPIRED";
export type ChallengeMyRole = "OBLIGATED" | "BENEFICIARY" | "ADMIN_REVIEW";

export type ChallengeSummary = {
  id: string;
  sourceGameSessionId: string;
  sourceRoomId: string;
  challengeTypeCode: string;
  challengeTypeDisplayName: string;
  challengeTypeDescription: string;
  status: ChallengeStatus;
  obligatedUserId: string;
  obligatedDisplayName: string;
  beneficiaryUserId: string;
  beneficiaryDisplayName: string;
  myRole: ChallengeMyRole;
  canResolve: boolean;
  respectPointsAwarded: number;
  karmaPointsAwarded: number;
  createdAt: string;
  resolvedAt: string | null;
  disputedAt: string | null;
  disputeNote: string | null;
  resolutionNote: string | null;
  resolvedByAdminId: string | null;
};

export type ChallengeListResponse = {
  totalCount: number;
  pendingCount: number;
  resolvedCount: number;
  challenges: ChallengeSummary[];
};
