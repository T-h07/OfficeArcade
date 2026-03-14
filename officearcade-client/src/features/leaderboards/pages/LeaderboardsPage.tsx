import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import { DepartmentsApiError, listDepartmentDirectory } from "../../departments/api/departmentsApi";
import type { DepartmentSummary } from "../../departments/types/departments.types";
import { PageHero } from "../../layout/PageHero";
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
    return "oa-chip-warning";
  }
  if (rank === 2) {
    return "oa-chip-info";
  }
  if (rank === 3) {
    return "oa-chip-route";
  }
  return "";
}

function podiumCardClass(rank: number) {
  if (rank === 1) {
    return "oa-podium-first";
  }
  if (rank === 2) {
    return "oa-podium-second";
  }
  if (rank === 3) {
    return "oa-podium-third";
  }
  return "";
}

function rowClass(entry: LeaderboardEntry) {
  if (entry.currentUser) {
    return "border-[rgba(var(--oa-route-rgb),0.46)] bg-[rgba(var(--oa-route-rgb),0.14)]";
  }
  return "border-oa-border bg-oa-surface-soft/55";
}

function formatMetric(entry: LeaderboardEntry) {
  return entry.primaryMetricDisplay;
}

function departmentBadgeClass(entry: LeaderboardEntry) {
  if (entry.department?.active) {
    return "oa-chip-route";
  }
  return "";
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
    <section className="oa-page">
      <PageHero
        kicker="Competitive Standings"
        title="Leaderboards"
        subtitle="Track rank, compare metrics, and push your position upward."
        rightSlot={
          leaderboard ? <span className="oa-chip">Refreshed {formatDateTime(leaderboard.generatedAt)}</span> : null
        }
        footerSlot={
          <>
            <span className="oa-chip">Scope: {selectedDepartmentLabel}</span>
            <span className="oa-chip oa-chip-route">Metric: {selectedOption?.title ?? selectedType}</span>
          </>
        }
      />

      {errorMessage ? (
        <div className="oa-alert oa-alert-danger">
          {errorMessage}
        </div>
      ) : null}

      {departmentFilterError ? (
        <div className="oa-alert oa-alert-danger">
          {departmentFilterError}
        </div>
      ) : null}

      <section className="oa-panel-soft">
        <div className="flex flex-wrap items-center gap-2">
          {typeOptions.map((option) => (
            <button
              key={option.type}
              type="button"
              onClick={() => setSelectedType(option.type)}
              className={`oa-btn px-3 py-1.5 text-xs ${
                selectedType === option.type
                  ? "oa-btn-primary"
                  : "oa-btn-ghost"
              }`}
              disabled={isLoading}
            >
              {option.title}
            </button>
          ))}

          <select
            value={selectedDepartmentFilter}
            onChange={(event) => setSelectedDepartmentFilter(event.target.value)}
            className="oa-select text-xs"
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
            className="oa-btn oa-btn-secondary ml-auto px-3 py-2 text-xs"
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
                className={`oa-podium-card ${podiumCardClass(entry.rank)} ${
                  entry.currentUser
                    ? "border-[rgba(var(--oa-route-rgb),0.5)] bg-[rgba(var(--oa-route-rgb),0.14)]"
                    : ""
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <span className={`oa-podium-rank ${rankBadgeClass(entry.rank)}`}>
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
                      <span className={`oa-chip ${departmentBadgeClass(entry)}`}>
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

          <section className="oa-panel">
            <h2 className="text-lg font-semibold text-oa-text">
              {leaderboard.title} Rankings
              <span className="ml-2 text-sm font-normal text-oa-muted">({selectedDepartmentLabel})</span>
            </h2>
            <p className="mt-1 text-sm text-oa-muted">Showing top {leaderboard.entries.length} eligible users.</p>

            <div className="mt-4 space-y-2">
              {leaderboard.entries.length === 0 ? (
                <p className="oa-empty-state">
                  {selectedDepartmentFilter === "ALL"
                    ? "No ranking data available for this leaderboard yet."
                    : "No ranking data available for the selected department filter yet."}
                </p>
              ) : (
                leaderboard.entries.map((entry) => (
                  <article
                    key={entry.userId}
                    className={`oa-room-card grid grid-cols-[auto_1fr_auto] items-center gap-3 px-3 py-2 ${rowClass(entry)}`}
                  >
                    <span className={`oa-podium-rank ${rankBadgeClass(entry.rank)}`}>
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
                          <span className={`oa-chip ${departmentBadgeClass(entry)}`}>
                            {entry.department
                              ? `${entry.department.displayName} (${entry.department.code})`
                              : "Unassigned Department"}
                          </span>
                        </p>
                      </div>
                    </div>

                    <div className="text-right">
                      <p className="text-base font-semibold text-oa-text">{formatMetric(entry)}</p>
                      <p className="text-xs text-oa-muted">{leaderboard.metricLabel}</p>
                    </div>
                  </article>
                ))
              )}
            </div>
          </section>

          <section className="oa-panel">
            <h2 className="text-lg font-semibold text-oa-text">Your Rank</h2>
            {leaderboard.currentUserEligible && leaderboard.currentUserEntry ? (
              <div className="oa-game-surface oa-game-connect-four-surface mt-3">
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
              <div className="oa-empty-state mt-3">
                {leaderboard.currentUserNote ?? "You are not currently eligible for this leaderboard."}
              </div>
            )}
          </section>
        </>
      ) : null}
    </section>
  );
}
