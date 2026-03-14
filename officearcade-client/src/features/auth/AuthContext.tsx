import { createContext, useContext, useEffect, useState } from "react";
import { fetchCurrentUser, loginWithCredentials } from "./authApi";
import { clearStoredAccessToken, getStoredAccessToken, storeAccessToken } from "./authStorage";
import type { AuthUser, LoginRequest } from "./auth.types";

type AuthStatus = "checking" | "authenticated" | "unauthenticated";

type AuthContextValue = {
  status: AuthStatus;
  user: AuthUser | null;
  accessToken: string | null;
  login: (request: LoginRequest) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

type AuthProviderProps = {
  children: React.ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
  const [status, setStatus] = useState<AuthStatus>("checking");
  const [user, setUser] = useState<AuthUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function restoreSession() {
      const token = getStoredAccessToken();
      if (!token) {
        if (!active) {
          return;
        }
        setStatus("unauthenticated");
        return;
      }

      try {
        const me = await fetchCurrentUser(token);
        if (!active) {
          return;
        }

        setUser(me.user);
        setAccessToken(token);
        setStatus("authenticated");
      } catch {
        if (!active) {
          return;
        }

        clearStoredAccessToken();
        setUser(null);
        setAccessToken(null);
        setStatus("unauthenticated");
      }
    }

    restoreSession();

    return () => {
      active = false;
    };
  }, []);

  async function login(request: LoginRequest) {
    const response = await loginWithCredentials(request);
    storeAccessToken(response.accessToken);
    setAccessToken(response.accessToken);
    setUser(response.user);
    setStatus("authenticated");
  }

  function logout() {
    clearStoredAccessToken();
    setAccessToken(null);
    setUser(null);
    setStatus("unauthenticated");
  }

  return (
    <AuthContext.Provider
      value={{
        status,
        user,
        accessToken,
        login,
        logout
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return context;
}
