import { useAuth } from "../../auth/AuthContext";
import { KpiTile } from "../components/KpiTile";
import { useEmployeeDashboard } from "../hooks/useEmployeeDashboard";

function formatDateTime(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

function clampPercentage(value: number) {
  return Math.max(0, Math.min(100, value));
}

function DashboardLoadingState() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="h-24 rounded-2xl border border-oa-border bg-oa-surface/70" />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <div key={index} className="h-24 rounded-xl border border-oa-border bg-oa-surface-soft/60" />
        ))}
      </div>
      <div className="h-36 rounded-2xl border border-oa-border bg-oa-surface/70" />
    </div>
  );
}

export function EmployeeDashboardPage() {
  const { accessToken, logout, user } = useAuth();
  const { dashboard, isLoading, errorMessage, refresh } = useEmployeeDashboard(accessToken, logout);

  if (!user) {
    return null;
  }

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Employee Home</p>
        <h1 className="mt-2 text-2xl font-semibold text-oa-text">Dashboard</h1>
        <p className="mt-2 text-sm text-oa-muted">
          Overview of progression, account status, and upcoming OfficeArcade systems.
        </p>
      </header>

      {isLoading ? <DashboardLoadingState /> : null}

      {!isLoading && errorMessage ? (
        <div className="rounded-2xl border border-oa-danger/40 bg-oa-danger/10 p-5">
          <h2 className="text-lg font-semibold text-oa-text">Dashboard unavailable</h2>
          <p className="mt-2 text-sm text-oa-muted">{errorMessage}</p>
          <button
            type="button"
            onClick={refresh}
            className="mt-4 rounded-lg border border-oa-border bg-oa-surface px-4 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/55"
          >
            Retry
          </button>
        </div>
      ) : null}

      {!isLoading && !errorMessage && dashboard ? (
        <div className="space-y-5">
          <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-sm text-oa-muted">Welcome back</p>
                <h2 className="mt-1 text-2xl font-semibold text-oa-text">{dashboard.displayName}</h2>
                <p className="mt-2 text-sm text-oa-muted">
                  Data refreshed at {formatDateTime(dashboard.generatedAt)}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <span className="rounded-full border border-oa-border bg-oa-surface-soft/70 px-3 py-1 text-xs font-medium text-oa-text">
                  {dashboard.role}
                </span>
                <span
                  className={`rounded-full border px-3 py-1 text-xs font-medium ${
                    dashboard.accountEnabled
                      ? "border-oa-accent/45 bg-oa-accent/15 text-oa-text"
                      : "border-oa-danger/45 bg-oa-danger/15 text-oa-danger"
                  }`}
                >
                  {dashboard.accountEnabled ? "Active" : "Inactive"}
                </span>
              </div>
            </div>
          </section>

          <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <KpiTile label="Level" value={`${dashboard.level}`} helperText="Current progression tier" />
            <KpiTile label="XP" value={`${dashboard.xp}`} helperText={`${dashboard.xpToNextLevel} to next level`} />
            <KpiTile
              label="Respect Points"
              value={`${dashboard.respectPoints}`}
              helperText="Reputation system foundation"
            />
            <KpiTile
              label="Karma Points"
              value={`${dashboard.karmaPoints}`}
              helperText="Balance indicator foundation"
            />
            <KpiTile label="Games Played" value={`${dashboard.gamesPlayed}`} helperText="Persisted profile total" />
            <KpiTile label="Wins" value={`${dashboard.wins}`} helperText="Completed match wins" />
            <KpiTile label="Losses" value={`${dashboard.losses}`} helperText="Completed match losses" />
            <KpiTile
              label="Win Rate"
              value={`${dashboard.winRatePercent.toFixed(1)}%`}
              helperText={`${dashboard.totalMatches} resolved matches`}
            />
          </section>

          <section className="rounded-2xl border border-oa-border bg-oa-surface/75 p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="text-lg font-semibold text-oa-text">Level Progress</h3>
                <p className="mt-1 text-sm text-oa-muted">
                  Level {dashboard.level} · {dashboard.xp} / {dashboard.xpForNextLevel} XP
                </p>
              </div>
              <p className="text-sm font-medium text-oa-text">{dashboard.xpProgressPercent.toFixed(1)}%</p>
            </div>

            <div className="mt-4 h-3 overflow-hidden rounded-full border border-oa-border bg-black/35">
              <div
                className="h-full rounded-full bg-gradient-to-r from-oa-accent/65 to-cyan-300/70 transition-[width] duration-500"
                style={{ width: `${clampPercentage(dashboard.xpProgressPercent)}%` }}
              />
            </div>
            <p className="mt-2 text-xs text-oa-muted">{dashboard.xpToNextLevel} XP remaining to reach next level.</p>
          </section>

          <section className="grid gap-4 xl:grid-cols-2">
            <article className="rounded-2xl border border-oa-border bg-oa-surface/75 p-5">
              <h3 className="text-lg font-semibold text-oa-text">Account Summary</h3>
              <div className="mt-3 space-y-2 text-sm text-oa-muted">
                <p>
                  <span className="font-medium text-oa-text">Email:</span> {dashboard.email}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Role:</span> {dashboard.role}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Profile Updated:</span>{" "}
                  {formatDateTime(dashboard.profileUpdatedAt)}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Enabled Games:</span>{" "}
                  {dashboard.enabledGameTypeCount}
                </p>
              </div>
            </article>

            <article className="rounded-2xl border border-oa-border bg-oa-surface/75 p-5">
              <h3 className="text-lg font-semibold text-oa-text">Platform Summary</h3>
              <p className="mt-2 text-sm text-oa-muted">
                Game catalog and profile metrics now come from persisted PostgreSQL data.
              </p>

              <div className="mt-4 flex flex-wrap gap-2">
                {dashboard.enabledGameTypes.map((gameType) => (
                  <span
                    key={gameType.code}
                    className="rounded-full border border-oa-border bg-oa-surface-soft/70 px-3 py-1 text-xs text-oa-text"
                  >
                    {gameType.displayName}
                  </span>
                ))}
              </div>

              <p className="mt-4 text-sm text-oa-muted">
                OA-PT06 will introduce the room and lobby foundation built on this dashboard baseline.
              </p>
            </article>
          </section>
        </div>
      ) : null}
    </section>
  );
}
