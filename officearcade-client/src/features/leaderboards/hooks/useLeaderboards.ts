import { useCallback, useEffect, useState } from "react";
import { getLeaderboard, getLeaderboardTypes, LeaderboardsApiError } from "../api/leaderboardsApi";
import type {
  LeaderboardResponse,
  LeaderboardType,
  LeaderboardTypeOption
} from "../types/leaderboards.types";

const DEFAULT_TYPE: LeaderboardType = "WINS";

type UseLeaderboardsResult = {
  selectedType: LeaderboardType;
  typeOptions: LeaderboardTypeOption[];
  leaderboard: LeaderboardResponse | null;
  isLoading: boolean;
  errorMessage: string | null;
  setSelectedType: (type: LeaderboardType) => void;
  refresh: () => Promise<void>;
};

function resolveErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallbackMessage;
}

export function useLeaderboards(accessToken: string | null, onUnauthorized: () => void): UseLeaderboardsResult {
  const [selectedType, setSelectedType] = useState<LeaderboardType>(DEFAULT_TYPE);
  const [typeOptions, setTypeOptions] = useState<LeaderboardTypeOption[]>([]);
  const [leaderboard, setLeaderboard] = useState<LeaderboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken) {
      setTypeOptions([]);
      setLeaderboard(null);
      setIsLoading(false);
      setErrorMessage(null);
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    try {
      const typesPayload = await getLeaderboardTypes(accessToken);
      const nextTypes = typesPayload.types;
      setTypeOptions(nextTypes);

      const activeType = nextTypes.some((entry) => entry.type === selectedType)
        ? selectedType
        : nextTypes[0]?.type ?? DEFAULT_TYPE;
      if (activeType !== selectedType) {
        setSelectedType(activeType);
      }

      const nextLeaderboard = await getLeaderboard(accessToken, activeType, 25);
      setLeaderboard(nextLeaderboard);
    } catch (error) {
      if (error instanceof LeaderboardsApiError && error.status === 401) {
        onUnauthorized();
        return;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to load leaderboards."));
      setLeaderboard(null);
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, onUnauthorized, selectedType]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return {
    selectedType,
    typeOptions,
    leaderboard,
    isLoading,
    errorMessage,
    setSelectedType,
    refresh
  };
}
