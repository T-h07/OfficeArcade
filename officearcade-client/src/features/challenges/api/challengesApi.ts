import { appConfig } from "../../../lib/config";
import type { ChallengeListResponse, ChallengeSummary } from "../types/challenges.types";

export class ChallengesApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ChallengesApiError";
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
    // Use fallback if payload is not parseable JSON.
  }

  throw new ChallengesApiError(response.status, message);
}

export async function listMyChallenges(token: string): Promise<ChallengeListResponse> {
  const response = await fetch(resolveUrl("/api/challenges/me"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load challenges.");
  }

  return (await response.json()) as ChallengeListResponse;
}

export async function getChallengeById(token: string, challengeId: string): Promise<ChallengeSummary> {
  const response = await fetch(resolveUrl(`/api/challenges/${challengeId}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load challenge.");
  }

  return (await response.json()) as ChallengeSummary;
}

export async function confirmChallenge(token: string, challengeId: string): Promise<ChallengeSummary> {
  const response = await fetch(resolveUrl(`/api/challenges/${challengeId}/confirm`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to confirm challenge.");
  }

  return (await response.json()) as ChallengeSummary;
}

export async function rejectChallenge(token: string, challengeId: string): Promise<ChallengeSummary> {
  const response = await fetch(resolveUrl(`/api/challenges/${challengeId}/reject`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to reject challenge.");
  }

  return (await response.json()) as ChallengeSummary;
}
