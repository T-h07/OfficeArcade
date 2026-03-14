import { useCallback, useEffect, useState } from "react";
import {
  closeLobbyRoom,
  createLobbyRoom,
  joinLobbyRoom,
  leaveLobbyRoom,
  listEnabledLobbyGameTypes,
  listLobbyRooms,
  LobbyApiError,
  getMyLobbyRoom
} from "../api/lobbyApi";
import { useLobbyRealtime } from "../realtime/useLobbyRealtime";
import type {
  CreateLobbyRoomRequest,
  JoinLobbyRoomRequest,
  LobbyGameType,
  LobbyRealtimeConnectionStatus,
  LobbyRealtimeEvent,
  LobbyRoomDetail,
  LobbyRoomSummary
} from "../types/lobby.types";

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallback;
}

type UseLobbyResult = {
  rooms: LobbyRoomSummary[];
  gameTypes: LobbyGameType[];
  myRoom: LobbyRoomDetail | null;
  isLoading: boolean;
  isMutating: boolean;
  realtimeStatus: LobbyRealtimeConnectionStatus;
  errorMessage: string | null;
  actionMessage: string | null;
  refresh: () => Promise<void>;
  createRoom: (request: CreateLobbyRoomRequest) => Promise<boolean>;
  joinRoom: (roomId: string, request: JoinLobbyRoomRequest) => Promise<boolean>;
  leaveRoom: (roomId: string) => Promise<boolean>;
  closeRoom: (roomId: string) => Promise<boolean>;
  clearActionMessage: () => void;
};

export function useLobby(accessToken: string | null, onUnauthorized: () => void): UseLobbyResult {
  const [rooms, setRooms] = useState<LobbyRoomSummary[]>([]);
  const [gameTypes, setGameTypes] = useState<LobbyGameType[]>([]);
  const [myRoom, setMyRoom] = useState<LobbyRoomDetail | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const loadLobbyData = useCallback(
    async (token: string, options?: { silent?: boolean }) => {
      const silent = options?.silent ?? false;
      if (!silent) {
        setIsLoading(true);
      }
      setErrorMessage(null);

      try {
        const [roomsResponse, myRoomResponse, gameTypeResponse] = await Promise.all([
          listLobbyRooms(token),
          getMyLobbyRoom(token),
          listEnabledLobbyGameTypes(token)
        ]);

        setRooms(roomsResponse.rooms);
        setMyRoom(myRoomResponse.room);
        setGameTypes(gameTypeResponse);
      } catch (error) {
        if (error instanceof LobbyApiError && error.status === 401) {
          onUnauthorized();
          return;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to load lobby data."));
      } finally {
        if (!silent) {
          setIsLoading(false);
        }
      }
    },
    [onUnauthorized]
  );

  const refresh = useCallback(async () => {
    if (!accessToken) {
      setRooms([]);
      setGameTypes([]);
      setMyRoom(null);
      setErrorMessage(null);
      setIsLoading(false);
      return;
    }

    const token = accessToken;
    await loadLobbyData(token, { silent: false });
  }, [accessToken, loadLobbyData]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const handleLobbyRealtimeEvent = useCallback(
    (_event: LobbyRealtimeEvent) => {
      if (!accessToken) {
        return;
      }

      void loadLobbyData(accessToken, { silent: true });
    },
    [accessToken, loadLobbyData]
  );

  const handleRoomRealtimeEvent = useCallback(
    (_event: LobbyRealtimeEvent) => {
      if (!accessToken) {
        return;
      }
      void loadLobbyData(accessToken, { silent: true });
    },
    [accessToken, loadLobbyData]
  );

  const realtimeStatus = useLobbyRealtime({
    accessToken,
    roomId: myRoom?.id ?? null,
    onLobbyEvent: handleLobbyRealtimeEvent,
    onRoomEvent: handleRoomRealtimeEvent
  });

  async function createRoomAction(request: CreateLobbyRoomRequest) {
    if (!accessToken) {
      return false;
    }
    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);

    try {
      await createLobbyRoom(accessToken, request);
      setActionMessage("Room created successfully.");
      await refresh();
      return true;
    } catch (error) {
      if (error instanceof LobbyApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to create room."));
      return false;
    } finally {
      setIsMutating(false);
    }
  }

  async function joinRoomAction(roomId: string, request: JoinLobbyRoomRequest) {
    if (!accessToken) {
      return false;
    }
    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);

    try {
      await joinLobbyRoom(accessToken, roomId, request);
      setActionMessage("Joined room.");
      await refresh();
      return true;
    } catch (error) {
      if (error instanceof LobbyApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to join room."));
      return false;
    } finally {
      setIsMutating(false);
    }
  }

  async function leaveRoomAction(roomId: string) {
    if (!accessToken) {
      return false;
    }
    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);

    try {
      const response = await leaveLobbyRoom(accessToken, roomId);
      setActionMessage(response.message);
      await refresh();
      return true;
    } catch (error) {
      if (error instanceof LobbyApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to leave room."));
      return false;
    } finally {
      setIsMutating(false);
    }
  }

  async function closeRoomAction(roomId: string) {
    if (!accessToken) {
      return false;
    }
    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);

    try {
      const response = await closeLobbyRoom(accessToken, roomId);
      setActionMessage(response.message);
      await refresh();
      return true;
    } catch (error) {
      if (error instanceof LobbyApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to close room."));
      return false;
    } finally {
      setIsMutating(false);
    }
  }

  return {
    rooms,
    gameTypes,
    myRoom,
    isLoading,
    isMutating,
    realtimeStatus,
    errorMessage,
    actionMessage,
    refresh,
    createRoom: createRoomAction,
    joinRoom: joinRoomAction,
    leaveRoom: leaveRoomAction,
    closeRoom: closeRoomAction,
    clearActionMessage: () => setActionMessage(null)
  };
}
