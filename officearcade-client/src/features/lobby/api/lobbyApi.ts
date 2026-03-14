import { appConfig } from "../../../lib/config";
import type {
  CreateLobbyRoomRequest,
  JoinLobbyRoomRequest,
  LobbyGameType,
  LobbyRoomActionResponse,
  LobbyRoomDetail,
  LobbyRoomListResponse,
  MyLobbyRoomResponse
} from "../types/lobby.types";

export class LobbyApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "LobbyApiError";
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
    // Use fallback when error payload is not parseable.
  }

  throw new LobbyApiError(response.status, message);
}

export async function listEnabledLobbyGameTypes(token: string): Promise<LobbyGameType[]> {
  const response = await fetch(resolveUrl("/api/lobby/game-types"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load game types.");
  }

  return (await response.json()) as LobbyGameType[];
}

export async function listLobbyRooms(token: string): Promise<LobbyRoomListResponse> {
  const response = await fetch(resolveUrl("/api/lobby/rooms"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load room list.");
  }

  return (await response.json()) as LobbyRoomListResponse;
}

export async function getMyLobbyRoom(token: string): Promise<MyLobbyRoomResponse> {
  const response = await fetch(resolveUrl("/api/lobby/my-room"), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load your room status.");
  }

  return (await response.json()) as MyLobbyRoomResponse;
}

export async function createLobbyRoom(token: string, request: CreateLobbyRoomRequest): Promise<LobbyRoomDetail> {
  const response = await fetch(resolveUrl("/api/lobby/rooms"), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to create room.");
  }

  return (await response.json()) as LobbyRoomDetail;
}

export async function joinLobbyRoom(
  token: string,
  roomId: string,
  request: JoinLobbyRoomRequest
): Promise<LobbyRoomDetail> {
  const response = await fetch(resolveUrl(`/api/lobby/rooms/${roomId}/join`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to join room.");
  }

  return (await response.json()) as LobbyRoomDetail;
}

export async function leaveLobbyRoom(token: string, roomId: string): Promise<LobbyRoomActionResponse> {
  const response = await fetch(resolveUrl(`/api/lobby/rooms/${roomId}/leave`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to leave room.");
  }

  return (await response.json()) as LobbyRoomActionResponse;
}

export async function closeLobbyRoom(token: string, roomId: string): Promise<LobbyRoomActionResponse> {
  const response = await fetch(resolveUrl(`/api/lobby/rooms/${roomId}/close`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to close room.");
  }

  return (await response.json()) as LobbyRoomActionResponse;
}
