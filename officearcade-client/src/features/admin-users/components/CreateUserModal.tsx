import { FormEvent, useEffect, useState } from "react";
import type { AppRole } from "../../auth/auth.types";
import type { CreateAdminUserRequest } from "../types/adminUsers.types";

type CreateUserModalProps = {
  isOpen: boolean;
  isSubmitting: boolean;
  errorMessage: string | null;
  onClose: () => void;
  onSubmit: (request: CreateAdminUserRequest) => Promise<void>;
};

const PASSWORD_HINT = "At least 8 characters with one letter and one number.";

export function CreateUserModal({ isOpen, isSubmitting, errorMessage, onClose, onSubmit }: CreateUserModalProps) {
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<AppRole>("EMPLOYEE");
  const [enabled, setEnabled] = useState(true);

  useEffect(() => {
    if (!isOpen) {
      return;
    }

    setEmail("");
    setDisplayName("");
    setPassword("");
    setRole("EMPLOYEE");
    setEnabled(true);
  }, [isOpen]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onSubmit({
      email,
      displayName,
      password,
      role,
      enabled
    });
  }

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 px-4 py-6">
      <section className="w-full max-w-xl rounded-2xl border border-oa-border bg-oa-surface p-6 shadow-glow">
        <header className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-oa-text">Create User Account</h2>
            <p className="mt-1 text-sm text-oa-muted">Add an account to the in-memory OfficeArcade user store.</p>
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
          <div>
            <label htmlFor="create-user-email" className="mb-1.5 block text-sm text-oa-muted">
              Email
            </label>
            <input
              id="create-user-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
              placeholder="name@officearcade.local"
              required
            />
          </div>

          <div>
            <label htmlFor="create-user-display-name" className="mb-1.5 block text-sm text-oa-muted">
              Display Name
            </label>
            <input
              id="create-user-display-name"
              type="text"
              value={displayName}
              onChange={(event) => setDisplayName(event.target.value)}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
              placeholder="Employee Display Name"
              minLength={2}
              maxLength={80}
              required
            />
          </div>

          <div>
            <label htmlFor="create-user-password" className="mb-1.5 block text-sm text-oa-muted">
              Initial Password
            </label>
            <input
              id="create-user-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
              minLength={8}
              maxLength={72}
              required
            />
            <p className="mt-1 text-xs text-oa-muted">{PASSWORD_HINT}</p>
          </div>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-[1fr_auto] sm:items-end">
            <div>
              <label htmlFor="create-user-role" className="mb-1.5 block text-sm text-oa-muted">
                Role
              </label>
              <select
                id="create-user-role"
                value={role}
                onChange={(event) => setRole(event.target.value as AppRole)}
                className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
              >
                <option value="EMPLOYEE">EMPLOYEE</option>
                <option value="ADMIN">ADMIN</option>
              </select>
            </div>

            <label className="inline-flex items-center gap-2 rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-muted">
              <input
                type="checkbox"
                checked={enabled}
                onChange={(event) => setEnabled(event.target.checked)}
                className="h-4 w-4 rounded border-oa-border bg-black/20 text-oa-accent focus:ring-oa-accent/30"
              />
              Enabled
            </label>
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
              {isSubmitting ? "Creating..." : "Create User"}
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
