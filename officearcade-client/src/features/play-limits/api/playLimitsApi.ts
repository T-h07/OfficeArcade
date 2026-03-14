import { appConfig } from "../../../lib/config";
import type { PlayLimitSummary } from "../types/playLimits.types";

export class PlayLimitsApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "PlayLimitsApiError";
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
    // Keep fallback message when payload is not parseable.
  }

  throw new PlayLimitsApiError(response.status, message);
}

export async function fetchMyPlayLimits(token: string): Promise<PlayLimitSummary> {
  const response = await fetch(resolveUrl("/api/play-limits/me"), {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`
    }
  });

  if (!response.ok) {
    await parseError(response, "Unable to load play-limit summary.");
  }

  return (await response.json()) as PlayLimitSummary;
}
