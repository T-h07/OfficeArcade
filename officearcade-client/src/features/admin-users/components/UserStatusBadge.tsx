type UserStatusBadgeProps = {
  enabled: boolean;
};

export function UserStatusBadge({ enabled }: UserStatusBadgeProps) {
  const className = enabled
    ? "border-emerald-400/45 bg-emerald-400/15 text-emerald-200"
    : "border-rose-400/45 bg-rose-400/15 text-rose-200";
  const label = enabled ? "Active" : "Inactive";

  return (
    <span className={`inline-flex rounded-full border px-2.5 py-0.5 text-xs font-medium ${className}`}>{label}</span>
  );
}
