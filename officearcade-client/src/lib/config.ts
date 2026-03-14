const DEFAULT_API_BASE_URL = "http://localhost:18180";
const DEFAULT_REALTIME_WS_URL = "ws://localhost:18180/ws";

function resolveApiBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL;
  if (!configured || configured.trim().length === 0) {
    return DEFAULT_API_BASE_URL;
  }
  return configured.trim().replace(/\/+$/, "");
}

function resolveRealtimeWsUrl(apiBaseUrl: string) {
  const configured = import.meta.env.VITE_REALTIME_WS_URL;
  if (configured && configured.trim().length > 0) {
    return configured.trim().replace(/\/+$/, "");
  }

  try {
    const url = new URL(apiBaseUrl);
    url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
    url.pathname = "/ws";
    url.search = "";
    url.hash = "";
    return url.toString().replace(/\/+$/, "");
  } catch {
    return DEFAULT_REALTIME_WS_URL;
  }
}

const apiBaseUrl = resolveApiBaseUrl();

export const appConfig = {
  apiBaseUrl,
  realtimeWsUrl: resolveRealtimeWsUrl(apiBaseUrl)
};
