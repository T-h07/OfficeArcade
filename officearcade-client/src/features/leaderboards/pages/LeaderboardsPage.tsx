import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import { DepartmentsApiError, listDepartmentDirectory } from "../../departments/api/departmentsApi";
import type { DepartmentSummary } from "../../departments/types/departments.types";
import { LeaderboardAvatarChip } from "../components/LeaderboardAvatarChip";
import { useLeaderboards } from "../hooks/useLeaderboards";
import type { LeaderboardEntry } from "../types/leaderboards.types";

function formatDateTime(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

function rankBadgeClass(rank: number) {
  if (rank === 1) {
    return "border-amber-300/60 bg-amber-300/20 text-amber-100";
  }
  if (rank === 2) {
    return "border-slate-300/60 bg-slate-300/20 text-slate-100";
  }
  if (rank === 3) {
    return "border-orange-300/60 bg-orange-300/20 text-orange-100";
  }
  return "border-oa-border bg-black/20 text-oa-muted";
}

function rowClass(entry: LeaderboardEntry) {
  if (entry.currentUser) {
    return "border-oa-accent/45 bg-oa-accent/10";
  }
  return "border-oa-border bg-oa-surface-soft/55";
}

function formatMetric(entry: LeaderboardEntry) {
  return entry.primaryMetricDisplay;
}

function departmentBadgeClass(entry: LeaderboardEntry) {
  if (entry.department?.active) {
    return "border-oa-accent/45 bg-oa-accent/15 text-oa-text";
  }
  return "border-oa-border bg-black/20 text-oa-muted";
}

export function LeaderboardsPage() {
  const { accessToken, logout, user } = useAuth();
  const {
    selectedType,
    selectedDepartmentFilter,
    typeOptions,
    leaderboard,
    isLoading,
    errorMessage,
    setSelectedType,
    setSelectedDepartmentFilter,
    refresh
  } = useLeaderboards(accessToken, logout);
  const [departments, setDepartments] = useState<DepartmentSummary[]>([]);
  const [isDepartmentFilterLoading, setIsDepartmentFilterLoading] = useState(false);
  const [departmentFilterError, setDepartmentFilterError] = useState<string | null>(null);

  const selectedOption = useMemo(
    () => typeOptions.find((option) => option.type === selectedType) ?? null,
    [typeOptions, selectedType]
  );

  useEffect(() => {
    async function loadDepartments() {
      if (!accessToken) {
        setDepartments([]);
        setDepartmentFilterError(null);
        setIsDepartmentFilterLoading(false);
        return;
      }

      setIsDepartmentFilterLoading(true);
      setDepartmentFilterError(null);
      try {
        const response = await listDepartmentDirectory(accessToken, false);
        setDepartments(
          response.departments.map((department) => ({
            id: department.id,
            code: department.code,
            displayName: department.displayName,
            active: department.active
          }))
        );
      } catch (error) {
        if (error instanceof DepartmentsApiError && error.status === 401) {
          logout();
          return;
        }
        setDepartmentFilterError("Unable to load departments for leaderboard filtering.");
      } finally {
        setIsDepartmentFilterLoading(false);
      }
    }

    void loadDepartments();
  }, [accessToken, logout]);

  const selectedDepartmentLabel = useMemo(() => {
    if (selectedDepartmentFilter === "ALL") {
      return "All Departments";
    }
    if (selectedDepartmentFilter === "UNASSIGNED") {
      return "Unassigned Users";
    }
    const selectedDepartment = departments.find((department) => department.id === selectedDepartmentFilter);
    if (!selectedDepartment) {
      return "Selected Department";
    }
    return `${selectedDepartment.displayName} (${selectedDepartment.code})`;
  }, [departments, selectedDepartmentFilter]);

  const topThree = leaderboard?.entries.slice(0, 3) ?? [];
  const currentUserVisibleInTop = leaderboard?.entries.some((entry) => entry.currentUser) ?? false;

  if (!user) {
    return null;
  }

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Competitive Standings</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-semibold text-oa-text">Leaderboards</h1>
          {leaderboard ? (
            <span className="rounded-full border border-oa-border bg-black/20 px-3 py-1 text-xs text-oa-muted">
              Refreshed {formatDateTime(leaderboard.generatedAt)}
            </span>
          ) : null}
        </div>
        <p className="mt-2 text-sm text-oa-muted">
          Compare standings across performance and reputation metrics. Rankings use persisted OfficeArcade data.
        </p>
        <p className="mt-1 text-xs text-oa-muted">Scope: {selectedDepartmentLabel}</p>
      </header>

      {errorMessage ? (
        <div className="rounded-xl border border-oa-danger/45 bg-oa-danger/10 px-4 py-3 text-sm text-oa-danger">
          {errorMessage}
        </div>
      ) : null}

      {departmentFilterError ? (
        <div className="rounded-xl border border-oa-danger/45 bg-oa-danger/10 px-4 py-3 text-sm text-oa-danger">
          {departmentFilterError}
        </div>
      ) : null}

      <section className="rounded-2xl border border-oa-border bg-oa-surface/82 p-4">
        <div className="flex flex-wrap items-center gap-2">
          {typeOptions.map((option) => (
            <button
              key={option.type}
              type="button"
              onClick={() => setSelectedType(option.type)}
              className={`rounded-full border px-3 py-1.5 text-xs font-medium transition-colors ${
                selectedType === option.type
                  ? "border-oa-accent/55 bg-oa-accent/15 text-oa-text"
                  : "border-oa-border bg-black/20 text-oa-muted hover:border-oa-accent/45 hover:text-oa-text"
              }`}
              disabled={isLoading}
            >
              {option.title}
            </button>
          ))}

          <select
            value={selectedDepartmentFilter}
            onChange={(event) => setSelectedDepartmentFilter(event.target.value)}
            className="rounded-md border border-oa-border bg-black/25 px-3 py-2 text-xs text-oa-text outline-none transition-colors focus:border-oa-accent/50"
            disabled={isLoading || isDepartmentFilterLoading}
          >
            <option value="ALL">All Departments</option>
            <option value="UNASSIGNED">Unassigned Users</option>
            {departments.map((department) => (
              <option key={department.id} value={department.id}>
                {department.displayName} ({department.code}){department.active ? "" : " - INACTIVE"}
              </option>
            ))}
          </select>

          <button
            type="button"
            onClick={() => {
              void refresh();
            }}
            className="ml-auto rounded-md border border-oa-border bg-black/25 px-3 py-2 text-xs text-oa-text transition-colors hover:border-oa-accent/50 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isLoading}
          >
            {isLoading ? "Refreshing..." : "Refresh"}
          </button>
        </div>

        {selectedOption ? (
          <div className="mt-3 rounded-lg border border-oa-border bg-black/20 p-3 text-sm text-oa-muted">
            <p className="text-oa-text">{selectedOption.description}</p>
            <p className="mt-1">
              Metric: <span className="text-oa-text">{selectedOption.metricLabel}</span> · Direction:{" "}
              <span className="text-oa-text">
                {selectedOption.rankingDirection === "DESC" ? "Higher is better" : "Lower is better"}
              </span>
              {selectedOption.minimumCompletedMatches > 0 ? (
                <>
                  {" "}
                  · Minimum completed matches:{" "}
                  <span className="text-oa-text">{selectedOption.minimumCompletedMatches}</span>
                </>
              ) : null}
            </p>
          </div>
        ) : null}
      </section>

      {leaderboard ? (
        <>
          <section className="grid gap-3 md:grid-cols-3">
            {topThree.map((entry) => (
              <article
                key={entry.userId}
                className={`rounded-xl border p-4 ${
                  entry.currentUser
                    ? "border-oa-accent/50 bg-oa-accent/12"
                    : "border-oa-border bg-oa-surface-soft/65"
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className={`rounded-full border px-2.5 py-1 text-xs font-semibold ${rankBadgeClass(entry.rank)}`}>
                    #{entry.rank}
                  </span>
                  <span className="text-xs text-oa-muted">{entry.role}</span>
                </div>
                <div className="mt-3 flex items-center gap-3">
                  <LeaderboardAvatarChip
                    displayName={entry.displayName}
                    profileFrameAssetKey={entry.profileFrameAssetKey}
                    badgeAssetKey={entry.badgeAssetKey}
                  />
                  <div>
                    <p className="text-sm font-semibold text-oa-text">{entry.displayName}</p>
                    <p className="text-xs text-oa-muted">
                      {leaderboard.metricLabel}: {formatMetric(entry)}
                    </p>
                    <p className="mt-1 text-xs">
                      <span className={`rounded-full border px-2 py-0.5 ${departmentBadgeClass(entry)}`}>
                        {entry.department
                          ? `${entry.department.displayName} (${entry.department.code})`
                          : "Unassigned Department"}
                      </span>
                    </p>
                  </div>
                </div>
              </article>
            ))}
          </section>

          <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
            <h2 className="text-lg font-semibold text-oa-text">
              {leaderboard.title} Rankings
              <span className="ml-2 text-sm font-normal text-oa-muted">({selectedDepartmentLabel})</span>
            </h2>
            <p className="mt-1 text-sm text-oa-muted">Showing top {leaderboard.entries.length} eligible users.</p>

            <div className="mt-4 space-y-2">
              {leaderboard.entries.length === 0 ? (
                <p className="rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-muted">
                  {selectedDepartmentFilter === "ALL"
                    ? "No ranking data available for this leaderboard yet."
                    : "No ranking data available for the selected department filter yet."}
                </p>
              ) : (
                leaderboard.entries.map((entry) => (
                  <article
                    key={entry.userId}
                    className={`grid grid-cols-[auto_1fr_auto] items-center gap-3 rounded-lg border px-3 py-2 ${rowClass(entry)}`}
                  >
                    <span className={`rounded-full border px-2 py-0.5 text-xs font-semibold ${rankBadgeClass(entry.rank)}`}>
                      #{entry.rank}
                    </span>

                    <div className="flex items-center gap-3">
                      <LeaderboardAvatarChip
                        displayName={entry.displayName}
                        profileFrameAssetKey={entry.profileFrameAssetKey}
                        badgeAssetKey={entry.badgeAssetKey}
                      />
                      <div>
                        <p className="text-sm font-medium text-oa-text">
                          {entry.displayName}
                          {entry.currentUser ? <span className="ml-2 text-xs text-oa-accent">(You)</span> : null}
                        </p>
                        <p className="text-xs text-oa-muted">
                          {entry.role} · Wins {entry.wins} · Win Rate {entry.winRatePercent.toFixed(1)}% · Level{" "}
                          {entry.level}
                        </p>
                        <p className="mt-0.5 text-xs">
                          <span className={`rounded-full border px-2 py-0.5 ${departmentBadgeClass(entry)}`}>
                            {entry.department
                              ? `${entry.department.displayName} (${entry.department.code})`
                              : "Unassigned Department"}
                          </span>
                        </p>
                      </div>
                    </div>

                    <div className="text-right">
                      <p className="text-sm font-semibold text-oa-text">{formatMetric(entry)}</p>
                      <p className="text-xs text-oa-muted">{leaderboard.metricLabel}</p>
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>

          <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
            <h2 className="text-lg font-semibold text-oa-text">Your Rank</h2>
            {leaderboard.currentUserEligible && leaderboard.currentUserEntry ? (
              <div className="mt-3 rounded-lg border border-oa-accent/45 bg-oa-accent/10 px-3 py-3">
                <p className="text-sm font-medium text-oa-text">
                  #{leaderboard.currentUserEntry.rank} in {leaderboard.title} ({selectedDepartmentLabel})
                </p>
                <p className="mt-1 text-sm text-oa-muted">
                  {leaderboard.metricLabel}: {leaderboard.currentUserEntry.primaryMetricDisplay}
                </p>
                {!currentUserVisibleInTop ? (
                  <p className="mt-1 text-xs text-oa-muted">
                    Your ranking is outside the visible top {leaderboard.limit} list.
                  </p>
                ) : null}
              </div>
            ) : (
              <div className="mt-3 rounded-lg border border-oa-border bg-black/20 px-3 py-3 text-sm text-oa-muted">
                {leaderboard.currentUserNote ?? "You are not currently eligible for this leaderboard."}
              </div>
            )}
          </section>
        </>
      ) : null}
    </section>
  );
}
