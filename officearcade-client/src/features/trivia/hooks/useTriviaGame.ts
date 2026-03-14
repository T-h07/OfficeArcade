import { useCallback, useEffect, useState } from "react";
import { getTriviaGameState, startTriviaGame, submitTriviaAnswer, TriviaApiError } from "../api/triviaApi";
import { useTriviaRealtime } from "../realtime/useTriviaRealtime";
import type {
  TriviaGameState,
  TriviaRealtimeConnectionStatus,
  TriviaRealtimeEvent
} from "../types/trivia.types";

type UseTriviaGameResult = {
  gameState: TriviaGameState | null;
  isLoading: boolean;
  isMutating: boolean;
  realtimeStatus: TriviaRealtimeConnectionStatus;
  errorMessage: string | null;
  actionMessage: string | null;
  refresh: () => Promise<void>;
  startGame: () => Promise<boolean>;
  submitAnswer: (selectedOptionIndex: number) => Promise<boolean>;
  clearActionMessage: () => void;
};

function resolveErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallbackMessage;
}

export function useTriviaGame(
  accessToken: string | null,
  roomId: string | null,
  enabled: boolean,
  onUnauthorized: () => void
): UseTriviaGameResult {
  const [gameState, setGameState] = useState<TriviaGameState | null>(null);
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
        const state = await getTriviaGameState(accessToken, roomId);
        setGameState(state);
      } catch (error) {
        if (error instanceof TriviaApiError && error.status === 401) {
          onUnauthorized();
          return;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to load Trivia Battle state."));
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
    (_event: TriviaRealtimeEvent) => {
      if (!enabled || !accessToken || !roomId) {
        return;
      }
      void loadState({ silent: true });
    },
    [enabled, accessToken, roomId, loadState]
  );

  const realtimeStatus = useTriviaRealtime({
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
      const nextState = await startTriviaGame(accessToken, roomId);
      setGameState(nextState);
      setActionMessage("Trivia Battle match started.");
      return true;
    } catch (error) {
      if (error instanceof TriviaApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to start Trivia Battle match."));
      return false;
    } finally {
      setIsMutating(false);
    }
  }, [enabled, accessToken, roomId, onUnauthorized]);

  const submitAnswer = useCallback(
    async (selectedOptionIndex: number) => {
      if (!enabled || !accessToken || !roomId) {
        return false;
      }
      setIsMutating(true);
      setErrorMessage(null);
      setActionMessage(null);

      try {
        const nextState = await submitTriviaAnswer(accessToken, roomId, { selectedOptionIndex });
        setGameState(nextState);
        setActionMessage("Answer submitted.");
        return true;
      } catch (error) {
        if (error instanceof TriviaApiError && error.status === 401) {
          onUnauthorized();
          return false;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to submit trivia answer."));
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
    submitAnswer,
    clearActionMessage: () => setActionMessage(null)
  };
}
