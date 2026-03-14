import { useEffect, useState } from "react";
import { fetchHealth } from "../lib/healthApi";
import type { HealthResponse, ServerConnectionStatus } from "../types/health";

type HealthState = {
  status: ServerConnectionStatus;
  payload: HealthResponse | null;
  errorMessage: string | null;
};

const INITIAL_STATE: HealthState = {
  status: "loading",
  payload: null,
  errorMessage: null
};

export function useHealthStatus() {
  const [state, setState] = useState<HealthState>(INITIAL_STATE);

  useEffect(() => {
    let active = true;

    async function checkServerHealth() {
      try {
        const payload = await fetchHealth();
        if (!active) {
          return;
        }
        setState({
          status: "online",
          payload,
          errorMessage: null
        });
      } catch (error) {
        if (!active) {
          return;
        }
        setState({
          status: "offline",
          payload: null,
          errorMessage: error instanceof Error ? error.message : "Unable to reach server."
        });
      }
    }

    checkServerHealth();

    return () => {
      active = false;
    };
  }, []);

  return state;
}
