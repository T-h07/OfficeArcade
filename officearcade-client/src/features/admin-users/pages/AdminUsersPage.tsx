import { FormEvent, useEffect, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import type { AppRole } from "../../auth/auth.types";
import {
  activateAdminUser,
  assignAdminUserDepartment,
  ApiError,
  createAdminUser,
  deactivateAdminUser,
  getAdminUserById,
  listAdminUsers,
  resetAdminUserPassword,
  updateAdminUser
} from "../api/adminUsersApi";
import { DepartmentsApiError, listAdminDepartments } from "../../departments/api/departmentsApi";
import { CreateUserModal } from "../components/CreateUserModal";
import { UserDetailPanel } from "../components/UserDetailPanel";
import { UserRoleBadge } from "../components/UserRoleBadge";
import { UserStatusBadge } from "../components/UserStatusBadge";
import type { AdminUser, CreateAdminUserRequest, UpdateAdminUserRequest } from "../types/adminUsers.types";
import type { DepartmentSummary } from "../../departments/types/departments.types";
import { PageHero } from "../../layout/PageHero";

type RoleFilter = "ALL" | AppRole;
type StatusFilter = "ALL" | "ACTIVE" | "INACTIVE";
type DepartmentFilter = "ALL" | "UNASSIGNED" | string;

function toErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

function toStatusFilterValue(filter: StatusFilter): boolean | undefined {
  if (filter === "ACTIVE") {
    return true;
  }
  if (filter === "INACTIVE") {
    return false;
  }
  return undefined;
}

function toRoleFilterValue(filter: RoleFilter): AppRole | undefined {
  return filter === "ALL" ? undefined : filter;
}

export function AdminUsersPage() {
  const { accessToken, logout, user } = useAuth();

  const [users, setUsers] = useState<AdminUser[]>([]);
  const [isListLoading, setIsListLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("ALL");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [departmentFilter, setDepartmentFilter] = useState<DepartmentFilter>("ALL");
  const [departments, setDepartments] = useState<DepartmentSummary[]>([]);
  const [isDepartmentsLoading, setIsDepartmentsLoading] = useState(false);

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isCreatingUser, setIsCreatingUser] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const [isDetailsOpen, setIsDetailsOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<AdminUser | null>(null);
  const [isDetailsLoading, setIsDetailsLoading] = useState(false);
  const [detailsError, setDetailsError] = useState<string | null>(null);
  const [detailsActionMessage, setDetailsActionMessage] = useState<string | null>(null);
  const [isSavingUser, setIsSavingUser] = useState(false);
  const [isTogglingUser, setIsTogglingUser] = useState(false);
  const [isResettingPassword, setIsResettingPassword] = useState(false);
  const [isAssigningDepartment, setIsAssigningDepartment] = useState(false);

  useEffect(() => {
    if (!accessToken) {
      return;
    }

    void loadUsers();
    void loadDepartments();
  }, [accessToken]);

  async function loadUsers(filters?: {
    search: string;
    roleFilter: RoleFilter;
    statusFilter: StatusFilter;
    departmentFilter: DepartmentFilter;
  }) {
    if (!accessToken) {
      return;
    }

    setIsListLoading(true);
    setListError(null);

    const resolvedSearch = filters?.search ?? search;
    const resolvedRoleFilter = filters?.roleFilter ?? roleFilter;
    const resolvedStatusFilter = filters?.statusFilter ?? statusFilter;
    const resolvedDepartmentFilter = filters?.departmentFilter ?? departmentFilter;

    try {
      const response = await listAdminUsers(accessToken, {
        search: resolvedSearch,
        role: toRoleFilterValue(resolvedRoleFilter),
        active: toStatusFilterValue(resolvedStatusFilter),
        departmentId: resolvedDepartmentFilter === "ALL" ? undefined : resolvedDepartmentFilter
      });
      setUsers(response.users);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        logout();
        return;
      }
      setListError(toErrorMessage(error, "Unable to load user management data."));
    } finally {
      setIsListLoading(false);
    }
  }

  async function loadDepartments() {
    if (!accessToken) {
      return;
    }

    setIsDepartmentsLoading(true);
    try {
      const response = await listAdminDepartments(accessToken);
      setDepartments(response.departments.map((department) => ({
        id: department.id,
        code: department.code,
        displayName: department.displayName,
        active: department.active
      })));
    } catch (error) {
      if (error instanceof DepartmentsApiError && error.status === 401) {
        logout();
        return;
      }
      setListError(toErrorMessage(error, "Unable to load departments."));
    } finally {
      setIsDepartmentsLoading(false);
    }
  }

  async function openDetailsForUser(userId: string) {
    if (!accessToken) {
      return;
    }

    setIsDetailsOpen(true);
    setIsDetailsLoading(true);
    setDetailsError(null);
    setDetailsActionMessage(null);

    try {
      const userDetails = await getAdminUserById(accessToken, userId);
      setSelectedUser(userDetails);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        logout();
        return;
      }
      setDetailsError(toErrorMessage(error, "Unable to load user details."));
    } finally {
      setIsDetailsLoading(false);
    }
  }

  async function handleCreateUser(request: CreateAdminUserRequest) {
    if (!accessToken) {
      return;
    }

    setIsCreatingUser(true);
    setCreateError(null);

    try {
      const created = await createAdminUser(accessToken, request);
      await loadUsers();
      setIsCreateModalOpen(false);
      await openDetailsForUser(created.id);
      setDetailsActionMessage(`Created ${created.email}.`);
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        logout();
        return;
      }
      setCreateError(toErrorMessage(error, "Unable to create user."));
    } finally {
      setIsCreatingUser(false);
    }
  }

  async function handleSaveSelectedUser(request: UpdateAdminUserRequest) {
    if (!accessToken || !selectedUser) {
      return;
    }

    setIsSavingUser(true);
    setDetailsError(null);
    setDetailsActionMessage(null);

    try {
      const updated = await updateAdminUser(accessToken, selectedUser.id, request);
      setSelectedUser(updated);
      setDetailsActionMessage("User profile updated.");
      await loadUsers();
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        logout();
        return;
      }
      setDetailsError(toErrorMessage(error, "Unable to update user."));
    } finally {
      setIsSavingUser(false);
    }
  }

  async function handleToggleSelectedUser() {
    if (!accessToken || !selectedUser) {
      return;
    }

    setIsTogglingUser(true);
    setDetailsError(null);
    setDetailsActionMessage(null);

    try {
      const updated = selectedUser.enabled
        ? await deactivateAdminUser(accessToken, selectedUser.id)
        : await activateAdminUser(accessToken, selectedUser.id);

      setSelectedUser(updated);
      setDetailsActionMessage(updated.enabled ? "User activated." : "User deactivated.");
      await loadUsers();
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        logout();
        return;
      }
      setDetailsError(toErrorMessage(error, "Unable to change user status."));
    } finally {
      setIsTogglingUser(false);
    }
  }

  async function handleResetSelectedUserPassword(newPassword: string) {
    if (!accessToken || !selectedUser) {
      return;
    }

    setIsResettingPassword(true);
    setDetailsError(null);
    setDetailsActionMessage(null);

    try {
      const response = await resetAdminUserPassword(accessToken, selectedUser.id, newPassword);
      setDetailsActionMessage(response.message);
      const refreshed = await getAdminUserById(accessToken, selectedUser.id);
      setSelectedUser(refreshed);
      await loadUsers();
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        logout();
        return;
      }
      setDetailsError(toErrorMessage(error, "Unable to reset user password."));
    } finally {
      setIsResettingPassword(false);
    }
  }

  async function handleAssignSelectedUserDepartment(departmentId: string | null) {
    if (!accessToken || !selectedUser) {
      return;
    }

    setIsAssigningDepartment(true);
    setDetailsError(null);
    setDetailsActionMessage(null);

    try {
      const updated = await assignAdminUserDepartment(accessToken, selectedUser.id, departmentId);
      setSelectedUser(updated);
      setDetailsActionMessage("Department assignment updated.");
      await loadUsers();
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        logout();
        return;
      }
      setDetailsError(toErrorMessage(error, "Unable to assign department."));
    } finally {
      setIsAssigningDepartment(false);
    }
  }

  function handleFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void loadUsers();
  }

  function handleClearFilters() {
    const reset = {
      search: "",
      roleFilter: "ALL" as RoleFilter,
      statusFilter: "ALL" as StatusFilter,
      departmentFilter: "ALL" as DepartmentFilter
    };

    setSearch(reset.search);
    setRoleFilter(reset.roleFilter);
    setStatusFilter(reset.statusFilter);
    setDepartmentFilter(reset.departmentFilter);
    void loadUsers(reset);
  }

  const visibleUsers = users;

  return (
    <section className="oa-page">
      <PageHero
        kicker="Admin Module"
        title="User Management"
        subtitle="Manage account access, role state, and department assignment."
        tone="admin"
        rightSlot={<span className="oa-chip oa-chip-info">Operations</span>}
      />

      <form className="oa-panel grid gap-3 lg:grid-cols-[1fr_180px_180px_220px_auto]" onSubmit={handleFilterSubmit}>
        <input
          type="text"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          className="oa-input"
          placeholder="Search email or display name"
        />

        <select
          value={roleFilter}
          onChange={(event) => setRoleFilter(event.target.value as RoleFilter)}
          className="oa-select"
        >
          <option value="ALL">All roles</option>
          <option value="ADMIN">ADMIN</option>
          <option value="EMPLOYEE">EMPLOYEE</option>
        </select>

        <select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
          className="oa-select"
        >
          <option value="ALL">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>

        <select
          value={departmentFilter}
          onChange={(event) => setDepartmentFilter(event.target.value as DepartmentFilter)}
          className="oa-select"
          disabled={isDepartmentsLoading}
        >
          <option value="ALL">All departments</option>
          <option value="UNASSIGNED">Unassigned only</option>
          {departments.map((department) => (
            <option key={department.id} value={department.id}>
              {department.displayName} ({department.code}){department.active ? "" : " - INACTIVE"}
            </option>
          ))}
        </select>

        <div className="flex gap-2">
          <button
            type="submit"
            className="oa-btn oa-btn-secondary px-3 py-2"
          >
            Apply
          </button>
          <button
            type="button"
            onClick={handleClearFilters}
            className="oa-btn oa-btn-ghost px-3 py-2"
          >
            Clear
          </button>
        </div>
      </form>

      <div className="oa-panel flex items-center justify-between px-4 py-3">
        <p className="text-sm text-oa-muted">
          {isListLoading ? "Loading users..." : `Showing ${visibleUsers.length} user${visibleUsers.length === 1 ? "" : "s"}`}
        </p>
        <button
          type="button"
          onClick={() => setIsCreateModalOpen(true)}
          className="oa-btn oa-btn-primary px-4 py-2"
        >
          Create User
        </button>
      </div>

      {listError ? (
        <p className="oa-alert oa-alert-danger">{listError}</p>
      ) : null}

      <div className="oa-panel overflow-x-auto p-0">
        <table className="min-w-full divide-y divide-oa-border text-sm">
          <thead className="bg-black/25 text-left text-xs uppercase tracking-[0.12em] text-oa-muted">
            <tr>
              <th className="px-4 py-3">Name</th>
              <th className="px-4 py-3">Email</th>
              <th className="px-4 py-3">Department</th>
              <th className="px-4 py-3">Role</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-oa-border/80">
            {isListLoading ? (
              <tr>
                <td colSpan={6} className="px-4 py-6 text-center text-oa-muted">
                  Loading users...
                </td>
              </tr>
            ) : visibleUsers.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-6 text-center text-oa-muted">
                  No users match the current filters.
                </td>
              </tr>
            ) : (
              visibleUsers.map((managedUser) => (
                <tr key={managedUser.id} className="hover:bg-black/25">
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-oa-text">{managedUser.displayName}</p>
                      {managedUser.id === user?.id ? <p className="text-xs text-oa-muted">Current session user</p> : null}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-oa-muted">{managedUser.email}</td>
                  <td className="px-4 py-3">
                    {managedUser.department ? (
                      <span
                        className={`oa-chip ${managedUser.department.active ? "oa-chip-route" : "oa-chip-danger"}`}
                      >
                        {managedUser.department.displayName}
                      </span>
                    ) : (
                      <span className="text-xs text-oa-muted">Unassigned</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <UserRoleBadge role={managedUser.role} />
                  </td>
                  <td className="px-4 py-3">
                    <UserStatusBadge enabled={managedUser.enabled} />
                  </td>
                  <td className="px-4 py-3">
                    <button
                      type="button"
                      onClick={() => {
                        void openDetailsForUser(managedUser.id);
                      }}
                      className="oa-btn oa-btn-ghost px-3 py-1.5 text-xs"
                    >
                      Manage
                    </button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <CreateUserModal
        isOpen={isCreateModalOpen}
        isSubmitting={isCreatingUser}
        errorMessage={createError}
        onClose={() => {
          if (isCreatingUser) {
            return;
          }
          setCreateError(null);
          setIsCreateModalOpen(false);
        }}
        onSubmit={handleCreateUser}
      />

      <UserDetailPanel
        isOpen={isDetailsOpen}
        user={selectedUser}
        isLoading={isDetailsLoading}
        isSaving={isSavingUser}
        isToggling={isTogglingUser}
        isResettingPassword={isResettingPassword}
        isAssigningDepartment={isAssigningDepartment}
        departments={departments}
        errorMessage={detailsError}
        actionMessage={detailsActionMessage}
        onClose={() => {
          if (isSavingUser || isTogglingUser || isResettingPassword || isAssigningDepartment) {
            return;
          }
          setIsDetailsOpen(false);
          setSelectedUser(null);
          setDetailsError(null);
          setDetailsActionMessage(null);
        }}
        onSave={handleSaveSelectedUser}
        onToggleActive={handleToggleSelectedUser}
        onResetPassword={handleResetSelectedUserPassword}
        onAssignDepartment={handleAssignSelectedUserDepartment}
      />
    </section>
  );
}
