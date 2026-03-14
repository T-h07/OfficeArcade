import { useAuth } from "./AuthContext";

type SuspendedAccessGateProps = {
  children: React.ReactNode;
};

function formatDateTime(value: string | null) {
  if (!value) {
    return "Unavailable";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

export function SuspendedAccessGate({ children }: SuspendedAccessGateProps) {
  const { user, logout } = useAuth();

  if (!user) {
    return null;
  }

  if (!user.suspended) {
    return <>{children}</>;
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-6 py-10">
      <section className="w-full max-w-xl rounded-2xl border border-oa-danger/45 bg-oa-surface/95 p-7 text-center shadow-glow">
        <p className="text-xs uppercase tracking-[0.16em] text-oa-muted">Account Status</p>
        <h1 className="mt-2 text-2xl font-semibold text-oa-text">Account Currently Suspended</h1>
        <p className="mt-3 text-sm text-oa-muted">
          Your account can no longer access normal gameplay and social features at this time.
        </p>
        <p className="mt-2 text-sm text-oa-muted">
          Contact your administrator if you need clarification or reactivation support.
        </p>

        <div className="mt-5 rounded-xl border border-oa-border bg-black/25 px-4 py-3 text-left text-sm">
          <p className="text-oa-muted">
            Suspended since: <span className="text-oa-text">{formatDateTime(user.suspendedAt)}</span>
          </p>
        </div>

        <button
          type="button"
          onClick={logout}
          className="mt-6 rounded-lg border border-oa-border bg-black/30 px-4 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/45"
        >
          Log Out
        </button>
      </section>
    </main>
  );
}
