import { appConfig } from "../../../lib/config";
import type { EmployeeDashboardResponse } from "../types/dashboard.types";

export class DashboardApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "DashboardApiError";
  }
}

function resolveUrl(path: string) {
  return `${appConfig.apiBaseUrl}${path}`;
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
    // Use fallback message when response payload cannot be parsed.
  }

  throw new DashboardApiError(response.status, message);
}

export async function fetchEmployeeDashboard(token: string): Promise<EmployeeDashboardResponse> {
  const response = await fetch(resolveUrl("/api/employee/dashboard"), {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`
    }
  });

  if (!response.ok) {
    await parseError(response, "Unable to load dashboard data.");
  }

  return (await response.json()) as EmployeeDashboardResponse;
}
