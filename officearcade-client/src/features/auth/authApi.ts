import { appConfig } from "../../lib/config";
import type { CurrentUserResponse, LoginRequest, LoginResponse } from "./auth.types";

const DEFAULT_HEADERS: HeadersInit = {
  Accept: "application/json",
  "Content-Type": "application/json"
};

function resolveErrorMessage(response: Response, fallback: string) {
  if (response.status === 401) {
    return "Invalid credentials. Please check email and password.";
  }
  if (response.status === 403) {
    return "You do not have permission to perform this action.";
  }
  return fallback;
}

export async function loginWithCredentials(requestBody: LoginRequest): Promise<LoginResponse> {
  const response = await fetch(`${appConfig.apiBaseUrl}/api/auth/login`, {
    method: "POST",
    headers: DEFAULT_HEADERS,
    body: JSON.stringify(requestBody)
  });

  if (!response.ok) {
    throw new Error(resolveErrorMessage(response, "Login failed. Please try again."));
  }

  return (await response.json()) as LoginResponse;
}

export async function fetchCurrentUser(token: string): Promise<CurrentUserResponse> {
  const response = await fetch(`${appConfig.apiBaseUrl}/api/auth/me`, {
    method: "GET",
    headers: {
      Accept: "application/json",
      Authorization: `Bearer ${token}`
    }
  });

  if (!response.ok) {
    throw new Error(resolveErrorMessage(response, "Session check failed."));
  }

  return (await response.json()) as CurrentUserResponse;
}
