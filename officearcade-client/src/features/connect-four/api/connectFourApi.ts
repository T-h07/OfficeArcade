import { appConfig } from "../../../lib/config";
import type { ConnectFourGameState, ConnectFourMoveRequest } from "../types/connectFour.types";

export class ConnectFourApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ConnectFourApiError";
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
    // Keep fallback message when payload is not parseable JSON.
  }

  throw new ConnectFourApiError(response.status, message);
}

export async function getConnectFourGameState(token: string, roomId: string): Promise<ConnectFourGameState> {
  const response = await fetch(resolveUrl(`/api/games/connect-four/room/${roomId}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load Connect Four game state.");
  }

  return (await response.json()) as ConnectFourGameState;
}

export async function startConnectFourGame(token: string, roomId: string): Promise<ConnectFourGameState> {
  const response = await fetch(resolveUrl(`/api/games/connect-four/room/${roomId}/start`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to start Connect Four match.");
  }

  return (await response.json()) as ConnectFourGameState;
}

export async function submitConnectFourMove(
  token: string,
  roomId: string,
  request: ConnectFourMoveRequest
): Promise<ConnectFourGameState> {
  const response = await fetch(resolveUrl(`/api/games/connect-four/room/${roomId}/move`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to submit move.");
  }

  return (await response.json()) as ConnectFourGameState;
}
