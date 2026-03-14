import { FormEvent, useEffect, useMemo, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import {
  activateAdminDepartment,
  createAdminDepartment,
  deactivateAdminDepartment,
  DepartmentsApiError,
  listAdminDepartments,
  updateAdminDepartment
} from "../api/departmentsApi";
import { DepartmentFormModal } from "../components/DepartmentFormModal";
import type { CreateDepartmentRequest, Department, UpdateDepartmentRequest } from "../types/departments.types";

type StatusFilter = "ALL" | "ACTIVE" | "INACTIVE";
type FormMode = "CREATE" | "EDIT";

function toErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

function statusBadgeClass(active: boolean) {
  if (active) {
    return "border-oa-accent/45 bg-oa-accent/15 text-oa-text";
  }
  return "border-oa-danger/45 bg-oa-danger/15 text-oa-danger";
}

function toDateTime(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

export function AdminDepartmentsPage() {
  const { accessToken, logout } = useAuth();

  const [departments, setDepartments] = useState<Department[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<FormMode>("CREATE");
  const [selectedDepartment, setSelectedDepartment] = useState<Department | null>(null);
  const [modalError, setModalError] = useState<string | null>(null);

  useEffect(() => {
    if (!accessToken) {
      return;
    }
    void loadDepartments();
  }, [accessToken]);

  async function loadDepartments(next?: { search: string; statusFilter: StatusFilter }) {
    if (!accessToken) {
      return;
    }

    const resolvedSearch = next?.search ?? search;
    const resolvedStatusFilter = next?.statusFilter ?? statusFilter;

    setIsLoading(true);
    setErrorMessage(null);

    try {
      const response = await listAdminDepartments(accessToken, {
        search: resolvedSearch,
        active:
          resolvedStatusFilter === "ALL"
            ? undefined
            : resolvedStatusFilter === "ACTIVE"
              ? true
              : false
      });
      setDepartments(response.departments);
    } catch (error) {
      if (error instanceof DepartmentsApiError && error.status === 401) {
        logout();
        return;
      }
      setErrorMessage(toErrorMessage(error, "Unable to load departments."));
    } finally {
      setIsLoading(false);
    }
  }

  async function handleCreateDepartment(request: CreateDepartmentRequest) {
    if (!accessToken) {
      return;
    }

    setIsMutating(true);
    setModalError(null);
    setActionMessage(null);
    try {
      await createAdminDepartment(accessToken, request);
      await loadDepartments();
      setIsModalOpen(false);
      setActionMessage("Department created.");
    } catch (error) {
      if (error instanceof DepartmentsApiError && error.status === 401) {
        logout();
        return;
      }
      setModalError(toErrorMessage(error, "Unable to create department."));
    } finally {
      setIsMutating(false);
    }
  }

  async function handleUpdateDepartment(departmentId: string, request: UpdateDepartmentRequest) {
    if (!accessToken) {
      return;
    }

    setIsMutating(true);
    setModalError(null);
    setActionMessage(null);
    try {
      await updateAdminDepartment(accessToken, departmentId, request);
      await loadDepartments();
      setIsModalOpen(false);
      setActionMessage("Department updated.");
    } catch (error) {
      if (error instanceof DepartmentsApiError && error.status === 401) {
        logout();
        return;
      }
      setModalError(toErrorMessage(error, "Unable to update department."));
    } finally {
      setIsMutating(false);
    }
  }

  async function handleToggleDepartment(department: Department) {
    if (!accessToken) {
      return;
    }
    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);
    try {
      if (department.active) {
        await deactivateAdminDepartment(accessToken, department.id);
        setActionMessage(`Department "${department.displayName}" deactivated.`);
      } else {
        await activateAdminDepartment(accessToken, department.id);
        setActionMessage(`Department "${department.displayName}" activated.`);
      }
      await loadDepartments();
    } catch (error) {
      if (error instanceof DepartmentsApiError && error.status === 401) {
        logout();
        return;
      }
      setErrorMessage(toErrorMessage(error, "Unable to change department status."));
    } finally {
      setIsMutating(false);
    }
  }

  function openCreateModal() {
    setModalMode("CREATE");
    setSelectedDepartment(null);
    setModalError(null);
    setIsModalOpen(true);
  }

  function openEditModal(department: Department) {
    setModalMode("EDIT");
    setSelectedDepartment(department);
    setModalError(null);
    setIsModalOpen(true);
  }

  function onFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    void loadDepartments();
  }

  const totals = useMemo(() => {
    let activeCount = 0;
    let inactiveCount = 0;
    let assignedUsers = 0;
    for (const department of departments) {
      if (department.active) {
        activeCount += 1;
      } else {
        inactiveCount += 1;
      }
      assignedUsers += department.assignedUserCount;
    }
    return {
      total: departments.length,
      activeCount,
      inactiveCount,
      assignedUsers
    };
  }, [departments]);

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Admin Segmentation</p>
        <h1 className="mt-2 text-2xl font-semibold text-oa-text">Departments</h1>
        <p className="mt-2 text-sm text-oa-muted">
          Create and manage department structure for company-ready user segmentation and filtering.
        </p>
      </header>

      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <article className="rounded-xl border border-oa-border bg-oa-surface-soft/65 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Total Departments</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{totals.total}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface-soft/65 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Active</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{totals.activeCount}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface-soft/65 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Inactive</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{totals.inactiveCount}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface-soft/65 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Assigned Users</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{totals.assignedUsers}</p>
        </article>
      </section>

      <form
        className="grid gap-3 rounded-2xl border border-oa-border bg-oa-surface/70 p-4 lg:grid-cols-[1fr_180px_auto]"
        onSubmit={onFilterSubmit}
      >
        <input
          type="text"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
          className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
          placeholder="Search by code or department name"
        />

        <select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
          className="rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
        >
          <option value="ALL">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>

        <div className="flex gap-2">
          <button
            type="submit"
            className="rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/50"
          >
            Apply
          </button>
          <button
            type="button"
            onClick={openCreateModal}
            className="rounded-lg border border-oa-accent/55 bg-oa-accent/25 px-4 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/35"
          >
            New Department
          </button>
        </div>
      </form>

      {errorMessage ? (
        <p className="rounded-lg border border-oa-danger/45 bg-oa-danger/10 px-3 py-2 text-sm text-oa-danger">
          {errorMessage}
        </p>
      ) : null}

      {actionMessage ? (
        <p className="rounded-lg border border-oa-accent/45 bg-oa-accent/10 px-3 py-2 text-sm text-oa-text">
          {actionMessage}
        </p>
      ) : null}

      <div className="overflow-x-auto rounded-2xl border border-oa-border bg-oa-surface/70">
        <table className="min-w-full divide-y divide-oa-border text-sm">
          <thead className="bg-black/25 text-left text-xs uppercase tracking-[0.12em] text-oa-muted">
            <tr>
              <th className="px-4 py-3">Department</th>
              <th className="px-4 py-3">Code</th>
              <th className="px-4 py-3">Description</th>
              <th className="px-4 py-3">Users</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3">Updated</th>
              <th className="px-4 py-3">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-oa-border/80">
            {isLoading ? (
              <tr>
                <td colSpan={7} className="px-4 py-6 text-center text-oa-muted">
                  Loading departments...
                </td>
              </tr>
            ) : departments.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-6 text-center text-oa-muted">
                  No departments found for the current filters.
                </td>
              </tr>
            ) : (
              departments.map((department) => (
                <tr key={department.id} className="hover:bg-black/25">
                  <td className="px-4 py-3 font-medium text-oa-text">{department.displayName}</td>
                  <td className="px-4 py-3 text-oa-muted">{department.code}</td>
                  <td className="max-w-[320px] px-4 py-3 text-oa-muted">{department.description ?? "-"}</td>
                  <td className="px-4 py-3 text-oa-text">{department.assignedUserCount}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full border px-2.5 py-0.5 text-xs ${statusBadgeClass(department.active)}`}>
                      {department.active ? "ACTIVE" : "INACTIVE"}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-oa-muted">{toDateTime(department.updatedAt)}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => openEditModal(department)}
                        className="rounded-lg border border-oa-border bg-black/20 px-3 py-1.5 text-xs font-medium text-oa-text transition-colors hover:border-oa-accent/50"
                      >
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          void handleToggleDepartment(department);
                        }}
                        className="rounded-lg border border-oa-border bg-black/20 px-3 py-1.5 text-xs font-medium text-oa-text transition-colors hover:border-oa-accent/50 disabled:cursor-not-allowed disabled:opacity-70"
                        disabled={isMutating}
                      >
                        {department.active ? "Deactivate" : "Activate"}
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <DepartmentFormModal
        isOpen={isModalOpen}
        mode={modalMode}
        isSubmitting={isMutating}
        errorMessage={modalError}
        department={selectedDepartment}
        onClose={() => {
          if (isMutating) {
            return;
          }
          setIsModalOpen(false);
          setSelectedDepartment(null);
          setModalError(null);
        }}
        onCreate={handleCreateDepartment}
        onUpdate={handleUpdateDepartment}
      />
    </section>
  );
}
