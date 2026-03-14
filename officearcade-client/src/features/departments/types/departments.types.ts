export type DepartmentSummary = {
  id: string;
  code: string;
  displayName: string;
  active: boolean;
};

export type Department = DepartmentSummary & {
  description: string | null;
  assignedUserCount: number;
  createdAt: string;
  updatedAt: string;
};

export type DepartmentListResponse = {
  total: number;
  departments: Department[];
};

export type CreateDepartmentRequest = {
  code: string;
  displayName: string;
  description: string | null;
};

export type UpdateDepartmentRequest = {
  code: string;
  displayName: string;
  description: string | null;
};
