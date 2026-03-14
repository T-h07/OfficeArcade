import { appConfig } from "../../../lib/config";
import type { ChallengeSummary } from "../../challenges/types/challenges.types";
import type {
  AdminChallengeReviewListResponse,
  ChallengeDisputeResolutionType,
  ChallengePolicyListResponse,
  ChallengePolicyUpdateRequest,
  CreateModerationReportRequest,
  ModerationAuditListResponse,
  ModerationReport,
  ModerationReportActionType,
  ModerationReportCategory,
  ModerationReportListResponse,
  ModerationReportStatus,
  ModerationUserStateResponse
} from "../types/moderation.types";

export class ModerationApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ModerationApiError";
  }
}

function resolveUrl(path: string) {
  return `${appConfig.apiBaseUrl}${path}`;
}

function authHeaders(token: string): HeadersInit {
  return {
    Accept: "application/json",
    Authorization: `Bearer ${token}`
  };
}

async function parseError(response: Response, fallbackMessage: string): Promise<never> {
  let message = fallbackMessage;

  try {
    const payload = (await response.json()) as Record<string, unknown>;
    if (typeof payload.detail === "string" && payload.detail.trim().length > 0) {
      message = payload.detail;
    } else if (typeof payload.message === "string" && payload.message.trim().length > 0) {
      message = payload.message;
    } else if (typeof payload.error === "string" && payload.error.trim().length > 0) {
      message = payload.error;
    }
  } catch {
    // Keep fallback message.
  }

  throw new ModerationApiError(response.status, message);
}

export async function submitModerationReport(
  token: string,
  request: CreateModerationReportRequest
): Promise<ModerationReport> {
  const response = await fetch(resolveUrl("/api/reports"), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to submit report.");
  }

  return (await response.json()) as ModerationReport;
}

export async function listMyModerationReports(token: string): Promise<ModerationReportListResponse> {
  const response = await fetch(resolveUrl("/api/reports/me"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load your reports.");
  }

  return (await response.json()) as ModerationReportListResponse;
}

type ListAdminReportsFilters = {
  status?: ModerationReportStatus;
  category?: ModerationReportCategory;
};

export async function listAdminModerationReports(
  token: string,
  filters: ListAdminReportsFilters
): Promise<ModerationReportListResponse> {
  const query = new URLSearchParams();
  if (filters.status) {
    query.set("status", filters.status);
  }
  if (filters.category) {
    query.set("category", filters.category);
  }

  const suffix = query.size > 0 ? `?${query.toString()}` : "";
  const response = await fetch(resolveUrl(`/api/admin/moderation/reports${suffix}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load moderation queue.");
  }

  return (await response.json()) as ModerationReportListResponse;
}

export async function getAdminModerationReportById(token: string, reportId: string): Promise<ModerationReport> {
  const response = await fetch(resolveUrl(`/api/admin/moderation/reports/${reportId}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load report details.");
  }

  return (await response.json()) as ModerationReport;
}

export async function dismissAdminModerationReport(
  token: string,
  reportId: string,
  note: string
): Promise<ModerationReport> {
  const response = await fetch(resolveUrl(`/api/admin/moderation/reports/${reportId}/dismiss`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ note })
  });

  if (!response.ok) {
    await parseError(response, "Unable to dismiss report.");
  }

  return (await response.json()) as ModerationReport;
}

export async function takeAdminModerationReportAction(
  token: string,
  reportId: string,
  actionType: ModerationReportActionType,
  note: string
): Promise<ModerationReport> {
  const response = await fetch(resolveUrl(`/api/admin/moderation/reports/${reportId}/take-action`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ actionType, note })
  });

  if (!response.ok) {
    await parseError(response, "Unable to apply moderation action.");
  }

  return (await response.json()) as ModerationReport;
}

export async function suspendModerationUser(
  token: string,
  userId: string,
  note: string
): Promise<ModerationUserStateResponse> {
  const response = await fetch(resolveUrl(`/api/admin/moderation/users/${userId}/suspend`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ note })
  });

  if (!response.ok) {
    await parseError(response, "Unable to suspend user.");
  }

  return (await response.json()) as ModerationUserStateResponse;
}

export async function unsuspendModerationUser(
  token: string,
  userId: string,
  note: string
): Promise<ModerationUserStateResponse> {
  const response = await fetch(resolveUrl(`/api/admin/moderation/users/${userId}/unsuspend`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ note })
  });

  if (!response.ok) {
    await parseError(response, "Unable to unsuspend user.");
  }

  return (await response.json()) as ModerationUserStateResponse;
}

export async function listAdminDisputedChallenges(token: string): Promise<AdminChallengeReviewListResponse> {
  const response = await fetch(resolveUrl("/api/admin/moderation/challenges"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load disputed challenges.");
  }

  return (await response.json()) as AdminChallengeReviewListResponse;
}

export async function resolveAdminDisputedChallenge(
  token: string,
  challengeId: string,
  resolutionType: ChallengeDisputeResolutionType,
  note: string
): Promise<ChallengeSummary> {
  const response = await fetch(resolveUrl(`/api/admin/moderation/challenges/${challengeId}/resolve`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ resolutionType, note })
  });

  if (!response.ok) {
    await parseError(response, "Unable to resolve disputed challenge.");
  }

  return (await response.json()) as ChallengeSummary;
}

export async function listModerationAuditEntries(token: string): Promise<ModerationAuditListResponse> {
  const response = await fetch(resolveUrl("/api/admin/moderation/audit"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load moderation audit history.");
  }

  return (await response.json()) as ModerationAuditListResponse;
}

export async function listChallengePolicies(token: string): Promise<ChallengePolicyListResponse> {
  const response = await fetch(resolveUrl("/api/admin/moderation/policies"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load challenge policies.");
  }

  return (await response.json()) as ChallengePolicyListResponse;
}

export async function updateChallengePolicies(
  token: string,
  request: ChallengePolicyUpdateRequest
): Promise<ChallengePolicyListResponse> {
  const response = await fetch(resolveUrl("/api/admin/moderation/policies"), {
    method: "PUT",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to update challenge policies.");
  }

  return (await response.json()) as ChallengePolicyListResponse;
}
