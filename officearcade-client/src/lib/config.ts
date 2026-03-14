const DEFAULT_API_BASE_URL = "http://localhost:18180";

function resolveApiBaseUrl() {
  const configured = import.meta.env.VITE_API_BASE_URL;
  if (!configured || configured.trim().length === 0) {
    return DEFAULT_API_BASE_URL;
  }
  return configured.trim().replace(/\/+$/, "");
}

export const appConfig = {
  apiBaseUrl: resolveApiBaseUrl()
};
