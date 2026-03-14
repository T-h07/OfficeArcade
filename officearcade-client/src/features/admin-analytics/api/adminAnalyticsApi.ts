import { appConfig } from "../../../lib/config";
import type {
  AdminAnalyticsDashboardResponse,
  AnalyticsDepartmentFilter,
  AnalyticsRange
} from "../types/adminAnalytics.types";

export class AdminAnalyticsApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "AdminAnalyticsApiError";
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
    // Keep fallback.
  }

  throw new AdminAnalyticsApiError(response.status, message);
}

export async function getAdminAnalyticsDashboard(
  token: string,
  range: AnalyticsRange,
  departmentFilter: AnalyticsDepartmentFilter
): Promise<AdminAnalyticsDashboardResponse> {
  const query = new URLSearchParams();
  query.set("range", range);
  if (departmentFilter !== "ALL") {
    query.set("departmentId", departmentFilter);
  }

  const response = await fetch(resolveUrl(`/api/admin/analytics/dashboard?${query.toString()}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load admin analytics dashboard.");
  }

  return (await response.json()) as AdminAnalyticsDashboardResponse;
}
