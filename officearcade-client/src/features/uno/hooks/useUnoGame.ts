import { useCallback, useEffect, useState } from "react";
import { drawUnoCard, getUnoGameState, playUnoCard, startUnoGame, UnoApiError } from "../api/unoApi";
import { useUnoRealtime } from "../realtime/useUnoRealtime";
import type { UnoGameState, UnoRealtimeConnectionStatus, UnoRealtimeEvent } from "../types/uno.types";

type UseUnoGameResult = {
  gameState: UnoGameState | null;
  isLoading: boolean;
  isMutating: boolean;
  realtimeStatus: UnoRealtimeConnectionStatus;
  errorMessage: string | null;
  actionMessage: string | null;
  refresh: () => Promise<void>;
  startGame: () => Promise<boolean>;
  playCard: (cardToken: string) => Promise<boolean>;
  drawCard: () => Promise<boolean>;
  clearActionMessage: () => void;
};

function resolveErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallbackMessage;
}

export function useUnoGame(
  accessToken: string | null,
  roomId: string | null,
  enabled: boolean,
  onUnauthorized: () => void
): UseUnoGameResult {
  const [gameState, setGameState] = useState<UnoGameState | null>(null);
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
        const state = await getUnoGameState(accessToken, roomId);
        setGameState(state);
      } catch (error) {
        if (error instanceof UnoApiError && error.status === 401) {
          onUnauthorized();
          return;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to load UNO game state."));
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
    (_event: UnoRealtimeEvent) => {
      if (!enabled || !accessToken || !roomId) {
        return;
      }
      void loadState({ silent: true });
    },
    [enabled, accessToken, roomId, loadState]
  );

  const realtimeStatus = useUnoRealtime({
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
      const nextState = await startUnoGame(accessToken, roomId);
      setGameState(nextState);
      setActionMessage("UNO match started.");
      return true;
    } catch (error) {
      if (error instanceof UnoApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to start UNO match."));
      return false;
    } finally {
      setIsMutating(false);
    }
  }, [enabled, accessToken, roomId, onUnauthorized]);

  const playCard = useCallback(
    async (cardToken: string) => {
      if (!enabled || !accessToken || !roomId) {
        return false;
      }
      setIsMutating(true);
      setErrorMessage(null);
      setActionMessage(null);

      try {
        const nextState = await playUnoCard(accessToken, roomId, { cardToken });
        setGameState(nextState);
        return true;
      } catch (error) {
        if (error instanceof UnoApiError && error.status === 401) {
          onUnauthorized();
          return false;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to play selected UNO card."));
        return false;
      } finally {
        setIsMutating(false);
      }
    },
    [enabled, accessToken, roomId, onUnauthorized]
  );

  const drawCard = useCallback(async () => {
    if (!enabled || !accessToken || !roomId) {
      return false;
    }
    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);

    try {
      const nextState = await drawUnoCard(accessToken, roomId);
      setGameState(nextState);
      setActionMessage("You drew one card and passed.");
      return true;
    } catch (error) {
      if (error instanceof UnoApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to draw UNO card."));
      return false;
    } finally {
      setIsMutating(false);
    }
  }, [enabled, accessToken, roomId, onUnauthorized]);

  return {
    gameState,
    isLoading,
    isMutating,
    realtimeStatus,
    errorMessage,
    actionMessage,
    refresh,
    startGame,
    playCard,
    drawCard,
    clearActionMessage: () => setActionMessage(null)
  };
}
