import { appConfig } from "../../../lib/config";
import type {
  CreateDepartmentRequest,
  DepartmentListResponse,
  UpdateDepartmentRequest
} from "../types/departments.types";

export class DepartmentsApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "DepartmentsApiError";
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
    // Keep fallback.
  }

  throw new DepartmentsApiError(response.status, message);
}

type ListAdminDepartmentsFilters = {
  search?: string;
  active?: boolean;
};

export async function listAdminDepartments(
  token: string,
  filters: ListAdminDepartmentsFilters = {}
): Promise<DepartmentListResponse> {
  const query = new URLSearchParams();
  if (filters.search && filters.search.trim().length > 0) {
    query.set("search", filters.search.trim());
  }
  if (typeof filters.active === "boolean") {
    query.set("active", String(filters.active));
  }
  const suffix = query.toString().length > 0 ? `?${query.toString()}` : "";

  const response = await fetch(resolveUrl(`/api/admin/departments${suffix}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load departments.");
  }

  return (await response.json()) as DepartmentListResponse;
}

export async function createAdminDepartment(token: string, request: CreateDepartmentRequest) {
  const response = await fetch(resolveUrl("/api/admin/departments"), {
    method: "POST",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to create department.");
  }

  return (await response.json()) as DepartmentListResponse["departments"][number];
}

export async function updateAdminDepartment(
  token: string,
  departmentId: string,
  request: UpdateDepartmentRequest
) {
  const response = await fetch(resolveUrl(`/api/admin/departments/${departmentId}`), {
    method: "PUT",
    headers: {
      ...authHeaders(token),
      "Content-Type": "application/json"
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    await parseError(response, "Unable to update department.");
  }

  return (await response.json()) as DepartmentListResponse["departments"][number];
}

export async function activateAdminDepartment(token: string, departmentId: string) {
  const response = await fetch(resolveUrl(`/api/admin/departments/${departmentId}/activate`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to activate department.");
  }

  return (await response.json()) as DepartmentListResponse["departments"][number];
}

export async function deactivateAdminDepartment(token: string, departmentId: string) {
  const response = await fetch(resolveUrl(`/api/admin/departments/${departmentId}/deactivate`), {
    method: "POST",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to deactivate department.");
  }

  return (await response.json()) as DepartmentListResponse["departments"][number];
}

export async function listDepartmentDirectory(
  token: string,
  activeOnly = true
): Promise<DepartmentListResponse> {
  const query = new URLSearchParams();
  query.set("activeOnly", String(activeOnly));

  const response = await fetch(resolveUrl(`/api/departments?${query.toString()}`), {
    method: "GET",
    headers: authHeaders(token)
  });

  if (!response.ok) {
    await parseError(response, "Unable to load department directory.");
  }

  return (await response.json()) as DepartmentListResponse;
}
