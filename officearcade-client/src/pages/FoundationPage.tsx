import { useHealthStatus } from "../hooks/useHealthStatus";
import { SystemStatusCard } from "../components/SystemStatusCard";

export function FoundationPage() {
  const { status, payload, errorMessage } = useHealthStatus();

  return (
    <main className="min-h-screen px-6 py-8 md:px-10">
      <div className="mx-auto flex w-full max-w-5xl flex-col gap-6">
        <header className="rounded-2xl border border-oa-border bg-oa-surface/90 p-6 shadow-glow">
          <p className="text-xs uppercase tracking-[0.2em] text-oa-muted">OfficeArcade Foundation</p>
          <h1 className="mt-2 text-3xl font-bold text-oa-text">OfficeArcade</h1>
          <p className="mt-3 max-w-3xl text-sm leading-relaxed text-oa-muted">
            Workplace-friendly desktop multiplayer platform foundation for short break-time sessions on managed
            company networks.
          </p>
        </header>

        <section className="grid grid-cols-1 gap-6 lg:grid-cols-[1.3fr_1fr]">
          <SystemStatusCard serverStatus={status} payload={payload} errorMessage={errorMessage} />

          <aside className="oa-card">
            <h2 className="text-lg font-semibold text-oa-text">Planned OA-PT Milestones</h2>
            <p className="mt-1 text-sm text-oa-muted">Scope intentionally limited to foundation setup in OA-PT01.</p>
            <ul className="mt-4 space-y-3 text-sm text-oa-text">
              <li className="rounded-lg border border-oa-border bg-oa-surface-soft/40 p-3">
                Admin tools coming in OA-PT02+
              </li>
              <li className="rounded-lg border border-oa-border bg-oa-surface-soft/40 p-3">
                Employee dashboard coming in OA-PT02+
              </li>
              <li className="rounded-lg border border-oa-border bg-oa-surface-soft/40 p-3">
                Multiplayer and game room flows scheduled for later PTs
              </li>
            </ul>
          </aside>
        </section>
      </div>
    </main>
  );
}
