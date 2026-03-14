import { appConfig } from "../../../lib/config";
import type { ProfileMeResponse } from "../types/profile.types";

export class ProfileApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ProfileApiError";
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
    // use fallback
  }
  throw new ProfileApiError(response.status, message);
}

export async function getMyProfile(token: string): Promise<ProfileMeResponse> {
  const response = await fetch(resolveUrl("/api/profile/me"), {
    method: "GET",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to load profile.");
  }
  return (await response.json()) as ProfileMeResponse;
}

export async function equipProfileItem(token: string, itemId: string): Promise<void> {
  const response = await fetch(resolveUrl(`/api/inventory/equip/${itemId}`), {
    method: "POST",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to equip selected item.");
  }
}

export async function unequipProfileItem(token: string, itemId: string): Promise<void> {
  const response = await fetch(resolveUrl(`/api/inventory/unequip/${itemId}`), {
    method: "POST",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to unequip selected item.");
  }
}
