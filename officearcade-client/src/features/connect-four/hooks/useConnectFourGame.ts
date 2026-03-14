import { useCallback, useEffect, useState } from "react";
import {
  ConnectFourApiError,
  getConnectFourGameState,
  startConnectFourGame,
  submitConnectFourMove
} from "../api/connectFourApi";
import { useConnectFourRealtime } from "../realtime/useConnectFourRealtime";
import type {
  ConnectFourGameState,
  ConnectFourRealtimeConnectionStatus,
  ConnectFourRealtimeEvent
} from "../types/connectFour.types";

type UseConnectFourGameResult = {
  gameState: ConnectFourGameState | null;
  isLoading: boolean;
  isMutating: boolean;
  realtimeStatus: ConnectFourRealtimeConnectionStatus;
  errorMessage: string | null;
  actionMessage: string | null;
  refresh: () => Promise<void>;
  startGame: () => Promise<boolean>;
  makeMove: (column: number) => Promise<boolean>;
  clearActionMessage: () => void;
};

function resolveErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallbackMessage;
}

export function useConnectFourGame(
  accessToken: string | null,
  roomId: string | null,
  enabled: boolean,
  onUnauthorized: () => void
): UseConnectFourGameResult {
  const [gameState, setGameState] = useState<ConnectFourGameState | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [isMutating, setIsMutating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const loadState = useCallback(
    async (options?: { silent?: boolean }) => {
      if (!enabled || !accessToken || !roomId) {
        setGameState(null);
        setErrorMessage(null);
        setIsLoading(false);
        return;
      }

      const silent = options?.silent ?? false;
      if (!silent) {
        setIsLoading(true);
      }
      setErrorMessage(null);

      try {
        const state = await getConnectFourGameState(accessToken, roomId);
        setGameState(state);
      } catch (error) {
        if (error instanceof ConnectFourApiError && error.status === 401) {
          onUnauthorized();
          return;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to load Connect Four state."));
      } finally {
        if (!silent) {
          setIsLoading(false);
        }
      }
    },
    [enabled, accessToken, roomId, onUnauthorized]
  );

  useEffect(() => {
    void loadState();
  }, [loadState]);

  const handleRealtimeEvent = useCallback(
    (_event: ConnectFourRealtimeEvent) => {
      if (!enabled || !accessToken || !roomId) {
        return;
      }
      void loadState({ silent: true });
    },
    [enabled, accessToken, roomId, loadState]
  );

  const realtimeStatus = useConnectFourRealtime({
    accessToken: enabled ? accessToken : null,
    roomId: enabled ? roomId : null,
    onEvent: handleRealtimeEvent
  });

  const refresh = useCallback(async () => {
    await loadState();
  }, [loadState]);

  const startGame = useCallback(async () => {
    if (!enabled || !accessToken || !roomId) {
      return false;
    }
    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);

    try {
      const nextState = await startConnectFourGame(accessToken, roomId);
      setGameState(nextState);
      setActionMessage("Connect Four match started.");
      return true;
    } catch (error) {
      if (error instanceof ConnectFourApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to start Connect Four match."));
      return false;
    } finally {
      setIsMutating(false);
    }
  }, [enabled, accessToken, roomId, onUnauthorized]);

  const makeMove = useCallback(
    async (column: number) => {
      if (!enabled || !accessToken || !roomId) {
        return false;
      }
      setIsMutating(true);
      setErrorMessage(null);
      setActionMessage(null);

      try {
        const nextState = await submitConnectFourMove(accessToken, roomId, { column });
        setGameState(nextState);
        return true;
      } catch (error) {
        if (error instanceof ConnectFourApiError && error.status === 401) {
          onUnauthorized();
          return false;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to place piece in selected column."));
        return false;
      } finally {
        setIsMutating(false);
      }
    },
    [enabled, accessToken, roomId, onUnauthorized]
  );

  return {
    gameState,
    isLoading,
    isMutating,
    realtimeStatus,
    errorMessage,
    actionMessage,
    refresh,
    startGame,
    makeMove,
    clearActionMessage: () => setActionMessage(null)
  };
}
