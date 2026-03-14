import type { AppRole } from "../../auth/auth.types";
import type { DepartmentSummary } from "../../departments/types/departments.types";

export type AdminUser = {
  id: string;
  email: string;
  displayName: string;
  role: AppRole;
  enabled: boolean;
  department: DepartmentSummary | null;
  createdAt: string;
  updatedAt: string;
};

export type AdminUserListResponse = {
  users: AdminUser[];
  total: number;
};

export type CreateAdminUserRequest = {
  email: string;
  displayName: string;
  password: string;
  role?: AppRole;
  enabled?: boolean;
};

export type UpdateAdminUserRequest = {
  email: string;
  displayName: string;
  role: AppRole;
};

export type AdminUserActionResponse = {
  status: string;
  message: string;
};

export type ListAdminUsersFilters = {
  search?: string;
  role?: AppRole;
  active?: boolean;
  departmentId?: string;
};
