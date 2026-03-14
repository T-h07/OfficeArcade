export type ChallengeStatus = "PENDING" | "COMPLETED_CONFIRMED" | "REJECTED" | "EXPIRED";
export type ChallengeMyRole = "OBLIGATED" | "BENEFICIARY";

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
};

export type ChallengeListResponse = {
  totalCount: number;
  pendingCount: number;
  resolvedCount: number;
  challenges: ChallengeSummary[];
};
