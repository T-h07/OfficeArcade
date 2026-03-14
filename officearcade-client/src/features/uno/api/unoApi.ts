import { appConfig } from "../../../lib/config";
import type { UnoGameState, UnoPlayCardRequest } from "../types/uno.types";

export class UnoApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "UnoApiError";
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
    // Keep fallback when payload is not parseable JSON.
  }

  throw new UnoApiError(response.status, message);
}

export async function getUnoGameState(token: string, roomId: string): Promise<UnoGameState> {
  const response = await fetch(resolveUrl(`/api/games/uno/room/${roomId}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load UNO game state.");
  }

  return (await response.json()) as UnoGameState;
}

export async function startUnoGame(token: string, roomId: string): Promise<UnoGameState> {
  const response = await fetch(resolveUrl(`/api/games/uno/room/${roomId}/start`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to start UNO match.");
  }

  return (await response.json()) as UnoGameState;
}

export async function playUnoCard(token: string, roomId: string, request: UnoPlayCardRequest): Promise<UnoGameState> {
  const response = await fetch(resolveUrl(`/api/games/uno/room/${roomId}/play`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to play selected UNO card.");
  }

  return (await response.json()) as UnoGameState;
}

export async function drawUnoCard(token: string, roomId: string): Promise<UnoGameState> {
  const response = await fetch(resolveUrl(`/api/games/uno/room/${roomId}/draw`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to draw UNO card.");
  }

  return (await response.json()) as UnoGameState;
}
