import type { AppRole } from "../../auth/auth.types";

type UserRoleBadgeProps = {
  role: AppRole;
};

export function UserRoleBadge({ role }: UserRoleBadgeProps) {
  const className =
    role === "ADMIN"
      ? "border-sky-400/45 bg-sky-400/15 text-sky-200"
      : "border-emerald-400/45 bg-emerald-400/15 text-emerald-200";

  return (
    <span className={`inline-flex rounded-full border px-2.5 py-0.5 text-xs font-medium ${className}`}>{role}</span>
  );
}
