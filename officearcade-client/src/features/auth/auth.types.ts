export type AppRole = "ADMIN" | "EMPLOYEE";

export type AuthUser = {
  id: string;
  email: string;
  displayName: string;
  role: AppRole;
  enabled: boolean;
  suspended: boolean;
  suspendedAt: string | null;
  suspensionNote: string | null;
};

export type LoginRequest = {
  email: string;
  password: string;
};

export type LoginResponse = {
  tokenType: string;
  accessToken: string;
  user: AuthUser;
};

export type CurrentUserResponse = {
  user: AuthUser;
};
