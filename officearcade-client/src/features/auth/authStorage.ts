const ACCESS_TOKEN_KEY = "officearcade.auth.access_token";

export function getStoredAccessToken(): string | null {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (!token || token.trim().length === 0) {
    return null;
  }
  return token;
}

export function storeAccessToken(token: string): void {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
}

export function clearStoredAccessToken(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
}
