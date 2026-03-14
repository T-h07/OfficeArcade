import { FormEvent, useEffect, useState } from "react";
import type { AppRole } from "../../auth/auth.types";
import type { AdminUser, UpdateAdminUserRequest } from "../types/adminUsers.types";
import { UserRoleBadge } from "./UserRoleBadge";
import { UserStatusBadge } from "./UserStatusBadge";

type UserDetailPanelProps = {
  isOpen: boolean;
  user: AdminUser | null;
  isLoading: boolean;
  isSaving: boolean;
  isToggling: boolean;
  isResettingPassword: boolean;
  errorMessage: string | null;
  actionMessage: string | null;
  onClose: () => void;
  onSave: (request: UpdateAdminUserRequest) => Promise<void>;
  onToggleActive: () => Promise<void>;
  onResetPassword: (newPassword: string) => Promise<void>;
};

function toDisplayTimestamp(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

export function UserDetailPanel({
  isOpen,
  user,
  isLoading,
  isSaving,
  isToggling,
  isResettingPassword,
  errorMessage,
  actionMessage,
  onClose,
  onSave,
  onToggleActive,
  onResetPassword
}: UserDetailPanelProps) {
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [role, setRole] = useState<AppRole>("EMPLOYEE");
  const [newPassword, setNewPassword] = useState("");

  useEffect(() => {
    if (!isOpen || !user) {
      return;
    }

    setEmail(user.email);
    setDisplayName(user.displayName);
    setRole(user.role);
    setNewPassword("");
  }, [isOpen, user]);

  async function handleSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onSave({
      email,
      displayName,
      role
    });
  }

  async function handleResetPassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!newPassword.trim()) {
      return;
    }
    await onResetPassword(newPassword);
    setNewPassword("");
  }

  if (!isOpen) {
    return null;
  }

  return (
    <div className="fixed inset-0 z-40 flex justify-end bg-black/55">
      <section className="h-full w-full max-w-xl overflow-y-auto border-l border-oa-border bg-oa-surface p-6">
        <header className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-oa-text">User Details</h2>
            <p className="mt-1 text-sm text-oa-muted">View and manage account state for this OfficeArcade user.</p>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md border border-oa-border px-3 py-1 text-sm text-oa-muted transition-colors hover:border-oa-accent/50 hover:text-oa-text"
          >
            Close
          </button>
        </header>

        {isLoading || !user ? (
          <div className="mt-6 rounded-xl border border-oa-border bg-black/25 p-4 text-sm text-oa-muted">
            Loading user details...
          </div>
        ) : (
          <div className="mt-6 space-y-5">
            <div className="rounded-xl border border-oa-border bg-black/20 p-4">
              <p className="text-xs uppercase tracking-[0.16em] text-oa-muted">Identity</p>
              <p className="mt-2 text-sm text-oa-text">{user.id}</p>
              <div className="mt-3 flex flex-wrap items-center gap-2">
                <UserRoleBadge role={user.role} />
                <UserStatusBadge enabled={user.enabled} />
              </div>
              <p className="mt-3 text-xs text-oa-muted">Created: {toDisplayTimestamp(user.createdAt)}</p>
              <p className="mt-1 text-xs text-oa-muted">Updated: {toDisplayTimestamp(user.updatedAt)}</p>
            </div>

            <form className="space-y-4 rounded-xl border border-oa-border bg-black/20 p-4" onSubmit={handleSave}>
              <h3 className="text-sm font-semibold text-oa-text">Edit Profile</h3>
              <div>
                <label htmlFor="user-detail-email" className="mb-1.5 block text-sm text-oa-muted">
                  Email
                </label>
                <input
                  id="user-detail-email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                  required
                />
              </div>
              <div>
                <label htmlFor="user-detail-display-name" className="mb-1.5 block text-sm text-oa-muted">
                  Display Name
                </label>
                <input
                  id="user-detail-display-name"
                  type="text"
                  value={displayName}
                  onChange={(event) => setDisplayName(event.target.value)}
                  className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                  minLength={2}
                  maxLength={80}
                  required
                />
              </div>
              <div>
                <label htmlFor="user-detail-role" className="mb-1.5 block text-sm text-oa-muted">
                  Role
                </label>
                <select
                  id="user-detail-role"
                  value={role}
                  onChange={(event) => setRole(event.target.value as AppRole)}
                  className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                >
                  <option value="EMPLOYEE">EMPLOYEE</option>
                  <option value="ADMIN">ADMIN</option>
                </select>
              </div>

              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isSaving}
                  className="rounded-lg border border-oa-accent/55 bg-oa-accent/25 px-4 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/35 disabled:cursor-not-allowed disabled:opacity-70"
                >
                  {isSaving ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </form>

            <div className="rounded-xl border border-oa-border bg-black/20 p-4">
              <h3 className="text-sm font-semibold text-oa-text">Account State</h3>
              <p className="mt-2 text-sm text-oa-muted">
                Deactivated accounts cannot authenticate until reactivated.
              </p>
              <button
                type="button"
                onClick={() => {
                  void onToggleActive();
                }}
                disabled={isToggling}
                className="mt-3 rounded-lg border border-oa-border bg-black/20 px-4 py-2 text-sm font-medium text-oa-text transition-colors hover:border-oa-accent/50 disabled:cursor-not-allowed disabled:opacity-70"
              >
                {isToggling ? "Applying..." : user.enabled ? "Deactivate User" : "Activate User"}
              </button>
            </div>

            <form className="space-y-3 rounded-xl border border-oa-border bg-black/20 p-4" onSubmit={handleResetPassword}>
              <h3 className="text-sm font-semibold text-oa-text">Reset Password</h3>
              <input
                type="password"
                value={newPassword}
                onChange={(event) => setNewPassword(event.target.value)}
                placeholder="New password"
                className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                minLength={8}
                maxLength={72}
                required
              />
              <p className="text-xs text-oa-muted">Use at least 8 characters with one letter and one number.</p>
              <div className="flex justify-end">
                <button
                  type="submit"
                  disabled={isResettingPassword}
                  className="rounded-lg border border-oa-border bg-black/20 px-4 py-2 text-sm font-medium text-oa-text transition-colors hover:border-oa-accent/50 disabled:cursor-not-allowed disabled:opacity-70"
                >
                  {isResettingPassword ? "Resetting..." : "Reset Password"}
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
          </div>
        )}
      </section>
    </div>
  );
}
