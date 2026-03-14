import { appConfig } from "../../../lib/config";
import type {
  LeaderboardResponse,
  LeaderboardType,
  LeaderboardTypeListResponse
} from "../types/leaderboards.types";

export class LeaderboardsApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "LeaderboardsApiError";
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

async function parseError(response: Response, fallback: string): Promise<never> {
  let message = fallback;
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
    // fallback
  }
  throw new LeaderboardsApiError(response.status, message);
}

export async function getLeaderboardTypes(token: string): Promise<LeaderboardTypeListResponse> {
  const response = await fetch(resolveUrl("/api/leaderboards/types"), {
    method: "GET",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to load leaderboard types.");
  }
  return (await response.json()) as LeaderboardTypeListResponse;
}

export async function getLeaderboard(
  token: string,
  type: LeaderboardType,
  limit = 25,
  departmentId?: string
): Promise<LeaderboardResponse> {
  const query = new URLSearchParams();
  query.set("type", type);
  query.set("limit", String(limit));
  if (departmentId && departmentId.trim().length > 0) {
    query.set("departmentId", departmentId.trim());
  }

  const response = await fetch(resolveUrl(`/api/leaderboards?${query.toString()}`), {
    method: "GET",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to load leaderboard.");
  }
  return (await response.json()) as LeaderboardResponse;
}
