import { useAuth } from "../../auth/AuthContext";
import { PageHero } from "../../layout/PageHero";
import { usePlayLimits } from "../../play-limits/hooks/usePlayLimits";
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

function formatDuration(seconds: number) {
  const safe = Math.max(seconds, 0);
  const hours = Math.floor(safe / 3600);
  const minutes = Math.floor((safe % 3600) / 60);
  const remainingSeconds = safe % 60;

  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }
  if (minutes > 0) {
    return `${minutes}m ${remainingSeconds}s`;
  }
  return `${remainingSeconds}s`;
}

function DashboardLoadingState() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="oa-panel h-24" />
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        {Array.from({ length: 8 }).map((_, index) => (
          <div key={index} className="oa-panel-soft h-24" />
        ))}
      </div>
      <div className="oa-panel h-36" />
    </div>
  );
}

export function EmployeeDashboardPage() {
  const { accessToken, logout, user } = useAuth();
  const { dashboard, isLoading, errorMessage, refresh } = useEmployeeDashboard(accessToken, logout);
  const {
    summary: playLimits,
    isLoading: isPlayLimitsLoading,
    errorMessage: playLimitsError,
    cooldownRemainingSecondsLive,
    refresh: refreshPlayLimits
  } = usePlayLimits(accessToken, logout);

  if (!user) {
    return null;
  }

  return (
    <section className="oa-page">
      <PageHero
        kicker="Employee Home"
        title="Dashboard"
        subtitle="Progress, cooldown status, and account momentum."
        rightSlot={<span className="oa-chip oa-chip-route">Player Overview</span>}
      />

      {isLoading ? <DashboardLoadingState /> : null}

      {!isLoading && errorMessage ? (
        <div className="oa-alert oa-alert-danger">
          <h2 className="text-lg font-semibold text-oa-text">Dashboard unavailable</h2>
          <p className="mt-2 text-sm text-oa-muted">{errorMessage}</p>
          <button
            type="button"
            onClick={refresh}
            className="oa-btn oa-btn-secondary mt-4 px-4 py-2"
          >
            Retry
          </button>
        </div>
      ) : null}

      {!isLoading && !errorMessage && dashboard ? (
        <div className="oa-page">
          <section className="oa-panel">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-sm text-oa-muted">Welcome back</p>
                <h2 className="mt-1 text-2xl font-semibold text-oa-text">{dashboard.displayName}</h2>
                <p className="oa-hero-subtitle">
                  Data refreshed at {formatDateTime(dashboard.generatedAt)}
                </p>
              </div>

              <div className="flex flex-wrap items-center gap-2">
                <span className="oa-chip">
                  {dashboard.role}
                </span>
                <span
                  className={`oa-chip ${dashboard.department?.active ? "oa-chip-route" : ""}`}
                >
                  {dashboard.department
                    ? `${dashboard.department.displayName} (${dashboard.department.code})`
                    : "Unassigned Department"}
                </span>
                <span
                  className={`oa-chip ${dashboard.accountEnabled ? "oa-chip-success" : "oa-chip-danger"}`}
                >
                  {dashboard.accountEnabled ? "Active" : "Inactive"}
                </span>
              </div>
            </div>
          </section>

          <section className="oa-panel">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Play Limits</p>
                <h3 className="mt-1 text-lg font-semibold text-oa-text">Daily Cap and Cooldown Status</h3>
              </div>
              <button
                type="button"
                onClick={refreshPlayLimits}
                className="oa-btn oa-btn-ghost px-3 py-1.5 text-xs"
              >
                Refresh Eligibility
              </button>
            </div>

            {isPlayLimitsLoading ? (
              <p className="mt-3 text-sm text-oa-muted">Loading play-limit status...</p>
            ) : null}

            {!isPlayLimitsLoading && playLimitsError ? (
              <p className="oa-alert oa-alert-danger mt-3">
                {playLimitsError}
              </p>
            ) : null}

            {!isPlayLimitsLoading && !playLimitsError && playLimits ? (
              <div className="mt-3 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <KpiTile label="Daily Limit" value={`${playLimits.dailyGameLimit}`} helperText="Completed matches/day" />
                <KpiTile label="Played Today" value={`${playLimits.gamesPlayedToday}`} helperText={`Date: ${playLimits.gamesPlayedDate}`} />
                <KpiTile label="Remaining" value={`${playLimits.gamesRemainingToday}`} helperText="Matches left today" />
                <KpiTile
                  label="Eligibility"
                  value={playLimits.canPlayNow ? "Ready" : "Blocked"}
                  helperText={playLimits.eligibilityReason}
                />
              </div>
            ) : null}

            {!isPlayLimitsLoading && !playLimitsError && playLimits && !playLimits.canPlayNow ? (
              <div className="oa-alert oa-alert-warning mt-3">
                {playLimits.eligibilityReason === "COOLDOWN_ACTIVE" ? (
                  <p>
                    Cooldown active. Next game available in <span className="font-semibold">{formatDuration(cooldownRemainingSecondsLive)}</span>.
                  </p>
                ) : (
                  <p>
                    Daily play limit reached. Reset at{" "}
                    <span className="font-semibold text-oa-text">{formatDateTime(playLimits.nextDailyResetAt)}</span>.
                  </p>
                )}
              </div>
            ) : null}
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

          <section className="oa-panel">
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
            <article className="oa-panel">
              <h3 className="text-lg font-semibold text-oa-text">Account Summary</h3>
              <div className="mt-3 space-y-2 text-sm text-oa-muted">
                <p>
                  <span className="font-medium text-oa-text">Email:</span> {dashboard.email}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Role:</span> {dashboard.role}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Department:</span>{" "}
                  {dashboard.department
                    ? `${dashboard.department.displayName} (${dashboard.department.code})`
                    : "Unassigned"}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Profile Updated:</span>{" "}
                  {formatDateTime(dashboard.profileUpdatedAt)}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Enabled Games:</span>{" "}
                  {dashboard.enabledGameTypeCount}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Pending Challenges:</span>{" "}
                  {dashboard.pendingChallengeCount}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Resolved Challenges:</span>{" "}
                  {dashboard.resolvedChallengeCount}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Owned Cosmetics:</span>{" "}
                  {dashboard.ownedCosmeticCount}
                </p>
                <p>
                  <span className="font-medium text-oa-text">Equipped Cosmetics:</span>{" "}
                  {dashboard.equippedCosmeticCount}
                </p>
              </div>
            </article>

            <article className="oa-panel">
              <h3 className="text-lg font-semibold text-oa-text">Platform Summary</h3>
              <p className="oa-hero-subtitle">
                Game catalog and profile metrics now come from persisted PostgreSQL data.
              </p>

              <div className="mt-4 flex flex-wrap gap-2">
                {dashboard.enabledGameTypes.map((gameType) => (
                  <span
                    key={gameType.code}
                    className="oa-chip"
                  >
                    {gameType.displayName}
                  </span>
                ))}
              </div>

              <p className="mt-4 text-sm text-oa-muted">
                Respect drives cosmetics. Karma stays non-currency and fairness-safe.
              </p>
            </article>
          </section>

          <section className="oa-panel">
            <h3 className="text-lg font-semibold text-oa-text">Equipped Cosmetic Loadout</h3>
            <p className="mt-1 text-sm text-oa-muted">
              Persisted equipped cosmetics from your inventory profile.
            </p>

            <div className="mt-3 space-y-2">
              {dashboard.equippedCosmetics.length === 0 ? (
                <p className="oa-empty-state">
                  No cosmetics equipped yet. Visit Store and Inventory to customize your loadout.
                </p>
              ) : (
                dashboard.equippedCosmetics.map((cosmetic) => (
                  <article
                    key={cosmetic.cosmeticItemId}
                    className="flex flex-wrap items-center justify-between gap-3 oa-panel-soft px-3 py-2 text-sm"
                  >
                    <div>
                      <p className="font-medium text-oa-text">{cosmetic.displayName}</p>
                      <p className="text-xs text-oa-muted">
                        {cosmetic.category} · {cosmetic.rarity}
                      </p>
                    </div>
                    <div className="text-right text-xs text-oa-muted">
                      <p className="text-oa-text">{cosmetic.previewAssetKey}</p>
                      <p>{formatDateTime(cosmetic.equippedAt)}</p>
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>

          <section className="oa-panel">
            <h3 className="text-lg font-semibold text-oa-text">Recent Challenge Activity</h3>
            <p className="mt-1 text-sm text-oa-muted">
              Post-match challenge history tied to completed Connect Four games.
            </p>

            <div className="mt-3 space-y-2">
              {dashboard.recentChallenges.length === 0 ? (
                <p className="oa-empty-state">
                  No challenge activity yet.
                </p>
              ) : (
                dashboard.recentChallenges.map((challenge) => (
                  <article
                    key={challenge.id}
                    className="flex flex-wrap items-center justify-between gap-3 oa-panel-soft px-3 py-2 text-sm"
                  >
                    <div>
                      <p className="font-medium text-oa-text">{challenge.challengeTypeDisplayName}</p>
                      <p className="text-xs text-oa-muted">
                        {challenge.myRole} vs {challenge.counterpartyDisplayName}
                      </p>
                    </div>
                    <div className="text-right text-xs text-oa-muted">
                      <p className="text-oa-text">{challenge.status}</p>
                      <p>{formatDateTime(challenge.createdAt)}</p>
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>
        </div>
      ) : null}
    </section>
  );
}

