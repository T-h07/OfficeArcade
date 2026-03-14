import type { HealthResponse, ServerConnectionStatus } from "../types/health";
import { StatusPill } from "./StatusPill";

type SystemStatusCardProps = {
  serverStatus: ServerConnectionStatus;
  payload: HealthResponse | null;
  errorMessage: string | null;
};

function resolveServerTone(serverStatus: ServerConnectionStatus): "neutral" | "success" | "danger" {
  if (serverStatus === "online") {
    return "success";
  }
  if (serverStatus === "offline") {
    return "danger";
  }
  return "neutral";
}

function resolveServerLabel(serverStatus: ServerConnectionStatus) {
  if (serverStatus === "online") {
    return "Server Reachable";
  }
  if (serverStatus === "offline") {
    return "Server Unreachable";
  }
  return "Checking Server";
}

export function SystemStatusCard({ serverStatus, payload, errorMessage }: SystemStatusCardProps) {
  return (
    <section className="oa-card">
      <div className="mb-5 flex items-start justify-between">
        <div>
          <h2 className="text-lg font-semibold text-oa-text">System Status</h2>
          <p className="mt-1 text-sm text-oa-muted">
            Initial client and backend connectivity diagnostics for OA-PT01.
          </p>
        </div>
        <StatusPill label="Client Ready" tone="success" />
      </div>

      <div className="space-y-4">
        <div className="flex items-center justify-between rounded-xl border border-oa-border bg-oa-surface-soft/60 p-3">
          <span className="text-sm text-oa-muted">Server Connection</span>
          <StatusPill label={resolveServerLabel(serverStatus)} tone={resolveServerTone(serverStatus)} />
        </div>

        {payload ? (
          <div className="rounded-xl border border-oa-border bg-black/25 p-3">
            <p className="text-xs uppercase tracking-wide text-oa-muted">Health Payload</p>
            <dl className="mt-3 grid grid-cols-1 gap-2 text-sm md:grid-cols-2">
              <div>
                <dt className="text-oa-muted">Status</dt>
                <dd className="font-medium text-oa-text">{payload.status}</dd>
              </div>
              <div>
                <dt className="text-oa-muted">Application</dt>
                <dd className="font-medium text-oa-text">{payload.application}</dd>
              </div>
              <div>
                <dt className="text-oa-muted">Profile</dt>
                <dd className="font-medium text-oa-text">{payload.profile}</dd>
              </div>
              <div>
                <dt className="text-oa-muted">Timestamp</dt>
                <dd className="font-medium text-oa-text">{payload.timestamp}</dd>
              </div>
            </dl>
          </div>
        ) : (
          <div className="rounded-xl border border-dashed border-oa-border bg-black/15 p-3 text-sm text-oa-muted">
            {serverStatus === "loading" ? "Waiting for backend response..." : errorMessage ?? "No data available."}
          </div>
        )}
      </div>
    </section>
  );
}
