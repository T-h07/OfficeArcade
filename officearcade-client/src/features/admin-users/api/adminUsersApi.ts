import { appConfig } from "../../../lib/config";
import type {
  AdminUser,
  AdminUserActionResponse,
  AdminUserListResponse,
  CreateAdminUserRequest,
  ListAdminUsersFilters,
  UpdateAdminUserRequest
} from "../types/adminUsers.types";

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}

function resolveUrl(path: string) {
  return `${appConfig.apiBaseUrl}${path}`;
}

function authHeaders(token: string): HeadersInit {
  return {
    Accept: "application/json",
    Authorization: `Bearer ${token}`
  };
}

async function parseError(response: Response, fallbackMessage: string): Promise<never> {
  let message = fallbackMessage;

  try {
    const payload = (await response.json()) as Record<string, unknown>;
    if (typeof payload.detail === "string" && payload.detail.trim().length > 0) {
      message = payload.detail;
    } else if (typeof payload.message === "string" && payload.message.trim().length > 0) {
      message = payload.message;
    } else if (typeof payload.error === "string" && payload.error.trim().length > 0) {
      message = payload.error;
    }
  } catch {
    // Use fallback message when response payload cannot be parsed.
  }

  throw new ApiError(response.status, message);
}

export async function listAdminUsers(token: string, filters: ListAdminUsersFilters): Promise<AdminUserListResponse> {
  const query = new URLSearchParams();
  if (filters.search && filters.search.trim().length > 0) {
    query.set("search", filters.search.trim());
  }
  if (filters.role) {
    query.set("role", filters.role);
  }
  if (typeof filters.active === "boolean") {
    query.set("active", String(filters.active));
  }
  if (filters.departmentId && filters.departmentId.trim().length > 0) {
    query.set("departmentId", filters.departmentId.trim());
  }

  const suffix = query.toString().length > 0 ? `?${query.toString()}` : "";
  const response = await fetch(resolveUrl(`/api/admin/users${suffix}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load users.");
  }

  return (await response.json()) as AdminUserListResponse;
}

export async function getAdminUserById(token: string, userId: string): Promise<AdminUser> {
  const response = await fetch(resolveUrl(`/api/admin/users/${userId}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load user details.");
  }

  return (await response.json()) as AdminUser;
}

export async function createAdminUser(token: string, request: CreateAdminUserRequest): Promise<AdminUser> {
  const response = await fetch(resolveUrl("/api/admin/users"), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to create user.");
  }

  return (await response.json()) as AdminUser;
}

export async function updateAdminUser(
  token: string,
  userId: string,
  request: UpdateAdminUserRequest
): Promise<AdminUser> {
  const response = await fetch(resolveUrl(`/api/admin/users/${userId}`), {
    method: "PUT",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to update user.");
  }

  return (await response.json()) as AdminUser;
}

export async function activateAdminUser(token: string, userId: string): Promise<AdminUser> {
  const response = await fetch(resolveUrl(`/api/admin/users/${userId}/activate`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to activate user.");
  }

  return (await response.json()) as AdminUser;
}

export async function deactivateAdminUser(token: string, userId: string): Promise<AdminUser> {
  const response = await fetch(resolveUrl(`/api/admin/users/${userId}/deactivate`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to deactivate user.");
  }

  return (await response.json()) as AdminUser;
}

export async function resetAdminUserPassword(
  token: string,
  userId: string,
  newPassword: string
): Promise<AdminUserActionResponse> {
  const response = await fetch(resolveUrl(`/api/admin/users/${userId}/reset-password`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ newPassword })
  });

  if (!response.ok) {
    await parseError(response, "Unable to reset password.");
  }

  return (await response.json()) as AdminUserActionResponse;
}

export async function assignAdminUserDepartment(
  token: string,
  userId: string,
  departmentId: string | null
): Promise<AdminUser> {
  const response = await fetch(resolveUrl(`/api/admin/users/${userId}/assign-department`), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ departmentId })
  });

  if (!response.ok) {
    await parseError(response, "Unable to assign department.");
  }

  return (await response.json()) as AdminUser;
}
