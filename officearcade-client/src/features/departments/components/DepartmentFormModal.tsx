import { FormEvent, useEffect, useState } from "react";
import type { CreateDepartmentRequest, Department, UpdateDepartmentRequest } from "../types/departments.types";

type DepartmentFormMode = "CREATE" | "EDIT";

type DepartmentFormModalProps = {
  isOpen: boolean;
  mode: DepartmentFormMode;
  isSubmitting: boolean;
  errorMessage: string | null;
  department: Department | null;
  onClose: () => void;
  onCreate: (request: CreateDepartmentRequest) => Promise<void>;
  onUpdate: (departmentId: string, request: UpdateDepartmentRequest) => Promise<void>;
};

export function DepartmentFormModal({
  isOpen,
  mode,
  isSubmitting,
  errorMessage,
  department,
  onClose,
  onCreate,
  onUpdate
}: DepartmentFormModalProps) {
  const [code, setCode] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [description, setDescription] = useState("");

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    if (mode === "EDIT" && department) {
      setCode(department.code);
      setDisplayName(department.displayName);
      setDescription(department.description ?? "");
      return;
    }
    setCode("");
    setDisplayName("");
    setDescription("");
  }, [department, isOpen, mode]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const payload = {
      code,
      displayName,
      description: description.trim().length > 0 ? description.trim() : null
    };

    if (mode === "CREATE") {
      await onCreate(payload);
      return;
    }

    if (!department) {
      return;
    }
    await onUpdate(department.id, payload);
  }

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 px-4 py-6">
      <section className="w-full max-w-2xl rounded-2xl border border-oa-border bg-oa-surface p-6 shadow-glow">
        <header className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-oa-text">
              {mode === "CREATE" ? "Create Department" : "Edit Department"}
            </h2>
            <p className="mt-1 text-sm text-oa-muted">
              Departments provide clean company segmentation for users and rankings.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md border border-oa-border px-3 py-1 text-sm text-oa-muted transition-colors hover:border-oa-accent/50 hover:text-oa-text"
          >
            Close
          </button>
        </header>

        <form className="mt-5 space-y-4" onSubmit={handleSubmit}>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label htmlFor="department-code" className="mb-1.5 block text-sm text-oa-muted">
                Code
              </label>
              <input
                id="department-code"
                type="text"
                value={code}
                onChange={(event) => setCode(event.target.value.toUpperCase())}
                className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                placeholder="ENGINEERING"
                minLength={2}
                maxLength={40}
                required
              />
            </div>

            <div>
              <label htmlFor="department-name" className="mb-1.5 block text-sm text-oa-muted">
                Display Name
              </label>
              <input
                id="department-name"
                type="text"
                value={displayName}
                onChange={(event) => setDisplayName(event.target.value)}
                className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                placeholder="Engineering"
                minLength={2}
                maxLength={120}
                required
              />
            </div>
          </div>

          <div>
            <label htmlFor="department-description" className="mb-1.5 block text-sm text-oa-muted">
              Description (Optional)
            </label>
            <textarea
              id="department-description"
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              className="min-h-[96px] w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
              maxLength={280}
              placeholder="Team focus or scope notes."
            />
          </div>

          {errorMessage ? (
            <p className="rounded-lg border border-oa-danger/45 bg-oa-danger/10 px-3 py-2 text-sm text-oa-danger">
              {errorMessage}
            </p>
          ) : null}

          <div className="flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-oa-border px-4 py-2 text-sm text-oa-muted transition-colors hover:border-oa-accent/45 hover:text-oa-text"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-lg border border-oa-accent/55 bg-oa-accent/25 px-4 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/35 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {isSubmitting ? "Saving..." : mode === "CREATE" ? "Create Department" : "Save Department"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
