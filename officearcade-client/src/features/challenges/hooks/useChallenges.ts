import { useCallback, useEffect, useState } from "react";
import {
  confirmChallenge,
  ChallengesApiError,
  listMyChallenges,
  rejectChallenge
} from "../api/challengesApi";
import type { ChallengeListResponse } from "../types/challenges.types";

type UseChallengesResult = {
  data: ChallengeListResponse | null;
  isLoading: boolean;
  isMutating: boolean;
  errorMessage: string | null;
  actionMessage: string | null;
  refresh: () => Promise<void>;
  confirm: (challengeId: string) => Promise<boolean>;
  reject: (challengeId: string) => Promise<boolean>;
  clearActionMessage: () => void;
};

function resolveErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallback;
}

export function useChallenges(accessToken: string | null, onUnauthorized: () => void): UseChallengesResult {
  const [data, setData] = useState<ChallengeListResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken) {
      setData(null);
      setErrorMessage(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    try {
      const nextData = await listMyChallenges(accessToken);
      setData(nextData);
    } catch (error) {
      if (error instanceof ChallengesApiError && error.status === 401) {
        onUnauthorized();
        return;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to load challenge data."));
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, onUnauthorized]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  const confirm = useCallback(
    async (challengeId: string) => {
      if (!accessToken) {
        return false;
      }
      setIsMutating(true);
      setErrorMessage(null);
      setActionMessage(null);

      try {
        await confirmChallenge(accessToken, challengeId);
        setActionMessage("Challenge confirmed. Respect awarded to obligated player.");
        await refresh();
        return true;
      } catch (error) {
        if (error instanceof ChallengesApiError && error.status === 401) {
          onUnauthorized();
          return false;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to confirm challenge."));
        return false;
      } finally {
        setIsMutating(false);
      }
    },
    [accessToken, onUnauthorized, refresh]
  );

  const reject = useCallback(
    async (challengeId: string) => {
      if (!accessToken) {
        return false;
      }
      setIsMutating(true);
      setErrorMessage(null);
      setActionMessage(null);

      try {
        await rejectChallenge(accessToken, challengeId);
        setActionMessage("Challenge rejected. Karma applied to obligated player.");
        await refresh();
        return true;
      } catch (error) {
        if (error instanceof ChallengesApiError && error.status === 401) {
          onUnauthorized();
          return false;
        }
        setErrorMessage(resolveErrorMessage(error, "Unable to reject challenge."));
        return false;
      } finally {
        setIsMutating(false);
      }
    },
    [accessToken, onUnauthorized, refresh]
  );

  return {
    data,
    isLoading,
    isMutating,
    errorMessage,
    actionMessage,
    refresh,
    confirm,
    reject,
    clearActionMessage: () => setActionMessage(null)
  };
}
