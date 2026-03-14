import { Link } from "react-router-dom";

export function ForbiddenPage() {
  return (
    <section className="rounded-2xl border border-oa-border bg-oa-surface/85 p-6">
      <h1 className="text-2xl font-semibold text-oa-text">Access Restricted</h1>
      <p className="mt-2 text-sm text-oa-muted">
        Your current role does not have access to this route in OA-PT03.
      </p>

      <Link
        to="/app/dashboard"
        className="mt-5 inline-flex rounded-lg border border-oa-border bg-oa-surface-soft/60 px-4 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/50"
      >
        Return to Dashboard
      </Link>
    </section>
  );
}
