import { useCallback, useEffect, useState } from "react";
import { AdminAnalyticsApiError, getAdminAnalyticsDashboard } from "../api/adminAnalyticsApi";
import type {
  AdminAnalyticsDashboardResponse,
  AnalyticsDepartmentFilter,
  AnalyticsRange
} from "../types/adminAnalytics.types";

type UseAdminAnalyticsResult = {
  dashboard: AdminAnalyticsDashboardResponse | null;
  isLoading: boolean;
  errorMessage: string | null;
  selectedRange: AnalyticsRange;
  selectedDepartmentFilter: AnalyticsDepartmentFilter;
  setSelectedRange: (value: AnalyticsRange) => void;
  setSelectedDepartmentFilter: (value: AnalyticsDepartmentFilter) => void;
  refresh: () => Promise<void>;
};

function toErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return "Unable to load admin analytics dashboard.";
}

export function useAdminAnalytics(
  accessToken: string | null,
  onUnauthorized: () => void
): UseAdminAnalyticsResult {
  const [dashboard, setDashboard] = useState<AdminAnalyticsDashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [selectedRange, setSelectedRange] = useState<AnalyticsRange>("7d");
  const [selectedDepartmentFilter, setSelectedDepartmentFilter] =
    useState<AnalyticsDepartmentFilter>("ALL");

  const refresh = useCallback(async () => {
    if (!accessToken) {
      setDashboard(null);
      setErrorMessage(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    try {
      const nextDashboard = await getAdminAnalyticsDashboard(
        accessToken,
        selectedRange,
        selectedDepartmentFilter
      );
      setDashboard(nextDashboard);
    } catch (error) {
      if (error instanceof AdminAnalyticsApiError && error.status === 401) {
        onUnauthorized();
        return;
      }
      setDashboard(null);
      setErrorMessage(toErrorMessage(error));
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, onUnauthorized, selectedDepartmentFilter, selectedRange]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  return {
    dashboard,
    isLoading,
    errorMessage,
    selectedRange,
    selectedDepartmentFilter,
    setSelectedRange,
    setSelectedDepartmentFilter,
    refresh
  };
}
