import { useCallback, useEffect, useMemo, useState } from "react";
import { fetchMyPlayLimits, PlayLimitsApiError } from "../api/playLimitsApi";
import type { PlayLimitSummary } from "../types/playLimits.types";

function toErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return "Unable to load play-limit status.";
}

type UsePlayLimitsResult = {
  summary: PlayLimitSummary | null;
  isLoading: boolean;
  errorMessage: string | null;
  cooldownRemainingSecondsLive: number;
  refresh: () => void;
};

export function usePlayLimits(accessToken: string | null, onUnauthorized: () => void): UsePlayLimitsResult {
  const [summary, setSummary] = useState<PlayLimitSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);
  const [nowMs, setNowMs] = useState(() => Date.now());
  const [autoRefreshTriggeredForCooldown, setAutoRefreshTriggeredForCooldown] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      setSummary(null);
      setIsLoading(false);
      setErrorMessage(null);
      return;
    }

    const token = accessToken;
    let active = true;
    setIsLoading(true);
    setErrorMessage(null);

    async function loadSummary() {
      try {
        const response = await fetchMyPlayLimits(token);
        if (!active) {
          return;
        }
        setSummary(response);
      } catch (error) {
        if (!active) {
          return;
        }
        if (error instanceof PlayLimitsApiError && error.status === 401) {
          onUnauthorized();
          return;
        }
        setSummary(null);
        setErrorMessage(toErrorMessage(error));
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    void loadSummary();

    return () => {
      active = false;
    };
  }, [accessToken, onUnauthorized, refreshToken]);

  useEffect(() => {
    if (!accessToken) {
      return;
    }
    const interval = setInterval(() => {
      setRefreshToken((current) => current + 1);
    }, 15000);
    return () => clearInterval(interval);
  }, [accessToken]);

  useEffect(() => {
    if (!summary?.cooldownActive || !summary.cooldownUntil) {
      return;
    }

    setAutoRefreshTriggeredForCooldown(null);
    const interval = setInterval(() => setNowMs(Date.now()), 1000);
    return () => clearInterval(interval);
  }, [summary?.cooldownActive, summary?.cooldownUntil]);

  const cooldownRemainingSecondsLive = useMemo(() => {
    if (!summary?.cooldownUntil) {
      return 0;
    }
    const cooldownUntilMs = Date.parse(summary.cooldownUntil);
    if (Number.isNaN(cooldownUntilMs)) {
      return 0;
    }
    return Math.max(Math.ceil((cooldownUntilMs - nowMs) / 1000), 0);
  }, [summary?.cooldownUntil, nowMs]);

  useEffect(() => {
    if (!summary?.cooldownActive || !summary.cooldownUntil) {
      return;
    }
    if (cooldownRemainingSecondsLive > 0) {
      return;
    }
    if (autoRefreshTriggeredForCooldown === summary.cooldownUntil) {
      return;
    }

    setAutoRefreshTriggeredForCooldown(summary.cooldownUntil);
    setRefreshToken((current) => current + 1);
  }, [
    summary?.cooldownActive,
    summary?.cooldownUntil,
    cooldownRemainingSecondsLive,
    autoRefreshTriggeredForCooldown
  ]);

  const refresh = useCallback(() => {
    setRefreshToken((current) => current + 1);
  }, []);

  return {
    summary,
    isLoading,
    errorMessage,
    cooldownRemainingSecondsLive,
    refresh
  };
}
