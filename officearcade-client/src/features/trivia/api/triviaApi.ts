import { appConfig } from "../../../lib/config";
import type { TriviaAnswerRequest, TriviaGameState } from "../types/trivia.types";

export class TriviaApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "TriviaApiError";
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

  throw new TriviaApiError(response.status, message);
}

export async function getTriviaGameState(token: string, roomId: string): Promise<TriviaGameState> {
  const response = await fetch(resolveUrl(`/api/games/trivia/room/${roomId}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load Trivia Battle game state.");
  }

  return (await response.json()) as TriviaGameState;
}

export async function startTriviaGame(token: string, roomId: string): Promise<TriviaGameState> {
  const response = await fetch(resolveUrl(`/api/games/trivia/room/${roomId}/start`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to start Trivia Battle match.");
  }

  return (await response.json()) as TriviaGameState;
}

export async function submitTriviaAnswer(
  token: string,
  roomId: string,
  request: TriviaAnswerRequest
): Promise<TriviaGameState> {
  const response = await fetch(resolveUrl(`/api/games/trivia/room/${roomId}/answer`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to submit trivia answer.");
  }

  return (await response.json()) as TriviaGameState;
}
