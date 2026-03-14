import { appConfig } from "./config";
import type { HealthResponse } from "../types/health";

const REQUEST_TIMEOUT_MS = 5000;

export async function fetchHealth(): Promise<HealthResponse> {
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${appConfig.apiBaseUrl}/api/health`, {
      method: "GET",
      signal: controller.signal,
      headers: {
        Accept: "application/json"
      }
    });

    if (!response.ok) {
      throw new Error(`Server returned HTTP ${response.status}`);
    }

    return (await response.json()) as HealthResponse;
  } catch (error) {
    if (error instanceof Error && error.name === "AbortError") {
      throw new Error("Request timed out while checking server health.");
    }

    throw error;
  } finally {
    window.clearTimeout(timeoutId);
  }
}
