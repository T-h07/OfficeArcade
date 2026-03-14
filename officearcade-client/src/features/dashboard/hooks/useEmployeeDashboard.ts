import { useEffect, useState } from "react";
import { DashboardApiError, fetchEmployeeDashboard } from "../api/dashboardApi";
import type { EmployeeDashboardResponse } from "../types/dashboard.types";

function toErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return "Unable to load dashboard data.";
}

export function useEmployeeDashboard(accessToken: string | null, onUnauthorized: () => void) {
  const [dashboard, setDashboard] = useState<EmployeeDashboardResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [refreshToken, setRefreshToken] = useState(0);

  useEffect(() => {
    if (!accessToken) {
      setDashboard(null);
      setIsLoading(false);
      setErrorMessage(null);
      return;
    }

    const token = accessToken;
    let active = true;
    setIsLoading(true);
    setErrorMessage(null);

    async function loadDashboard() {
      try {
        const response = await fetchEmployeeDashboard(token);
        if (!active) {
          return;
        }
        setDashboard(response);
      } catch (error) {
        if (!active) {
          return;
        }

        if (error instanceof DashboardApiError && error.status === 401) {
          onUnauthorized();
          return;
        }

        setDashboard(null);
        setErrorMessage(toErrorMessage(error));
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    }

    void loadDashboard();

    return () => {
      active = false;
    };
  }, [accessToken, onUnauthorized, refreshToken]);

  function refresh() {
    setRefreshToken((current) => current + 1);
  }

  return {
    dashboard,
    isLoading,
    errorMessage,
    refresh
  };
}
