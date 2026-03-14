import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import { DepartmentsApiError, listDepartmentDirectory } from "../../departments/api/departmentsApi";
import type { DepartmentSummary } from "../../departments/types/departments.types";
import { useAdminAnalytics } from "../hooks/useAdminAnalytics";
import type {
  AnalyticsActivityPoint,
  AnalyticsDepartmentInsight,
  AnalyticsRange
} from "../types/adminAnalytics.types";

type MetricCardProps = {
  label: string;
  value: string;
  helperText: string;
};

function MetricCard({ label, value, helperText }: MetricCardProps) {
  return (
    <article className="rounded-xl border border-oa-border bg-oa-surface-soft/70 p-4">
      <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-oa-text">{value}</p>
      <p className="mt-1 text-xs text-oa-muted">{helperText}</p>
    </article>
  );
}

type LineTrendCardProps = {
  title: string;
  colorClass: string;
  points: AnalyticsActivityPoint[];
  valueSelector: (point: AnalyticsActivityPoint) => number;
};

function LineTrendCard({ title, colorClass, points, valueSelector }: LineTrendCardProps) {
  const values = points.map((point) => valueSelector(point));
  const maxValue = Math.max(1, ...values);
  const chartHeight = 130;
  const chartWidth = 320;
  const yPadding = 16;
  const xPadding = 12;
  const innerWidth = chartWidth - xPadding * 2;
  const innerHeight = chartHeight - yPadding * 2;

  const polylinePoints = values
    .map((value, index) => {
      const x = xPadding + (values.length === 1 ? innerWidth / 2 : (index / (values.length - 1)) * innerWidth);
      const y = yPadding + innerHeight - (value / maxValue) * innerHeight;
      return `${x},${y}`;
    })
    .join(" ");

  const latestValue = values.length > 0 ? values[values.length - 1] : 0;

  return (
    <article className="rounded-xl border border-oa-border bg-oa-surface-soft/60 p-4">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-sm font-semibold text-oa-text">{title}</h3>
        <p className="text-sm text-oa-muted">Latest: {latestValue.toLocaleString()}</p>
      </div>

      {values.length === 0 ? (
        <p className="mt-4 rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-xs text-oa-muted">
          No trend data available.
        </p>
      ) : (
        <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="mt-4 h-32 w-full">
          <line x1={xPadding} y1={chartHeight - yPadding} x2={chartWidth - xPadding} y2={chartHeight - yPadding} className="stroke-oa-border/80" />
          <line x1={xPadding} y1={yPadding} x2={xPadding} y2={chartHeight - yPadding} className="stroke-oa-border/80" />
          <polyline
            points={polylinePoints}
            fill="none"
            className={`stroke-2 ${colorClass}`}
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        </svg>
      )}
    </article>
  );
}

function formatDateTime(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

function toRangeLabel(value: AnalyticsRange) {
  if (value === "today") {
    return "Today";
  }
  if (value === "7d") {
    return "Last 7 Days";
  }
  if (value === "30d") {
    return "Last 30 Days";
  }
  return "All Time";
}

function toDepartmentLabel(department: AnalyticsDepartmentInsight) {
  if (department.unassignedBucket) {
    return "Unassigned";
  }
  return `${department.departmentDisplayName} (${department.departmentCode})`;
}

export function AdminAnalyticsPage() {
  const { accessToken, logout } = useAuth();
  const {
    dashboard,
    isLoading,
    errorMessage,
    selectedRange,
    selectedDepartmentFilter,
    setSelectedRange,
    setSelectedDepartmentFilter,
    refresh
  } = useAdminAnalytics(accessToken, logout);

  const [departments, setDepartments] = useState<DepartmentSummary[]>([]);
  const [departmentLoadError, setDepartmentLoadError] = useState<string | null>(null);

  useEffect(() => {
    async function loadDepartments() {
      if (!accessToken) {
        setDepartments([]);
        setDepartmentLoadError(null);
        return;
      }

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
        setDepartmentLoadError(null);
      } catch (error) {
        if (error instanceof DepartmentsApiError && error.status === 401) {
          logout();
          return;
        }
        setDepartmentLoadError("Unable to load departments for analytics filter.");
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
    const selected = departments.find((department) => department.id === selectedDepartmentFilter);
    return selected ? `${selected.displayName} (${selected.code})` : "Selected Department";
  }, [departments, selectedDepartmentFilter]);

  const topGame = dashboard?.gameUsage[0] ?? null;
  const maxDepartmentParticipation = useMemo(() => {
    if (!dashboard || dashboard.departments.length === 0) {
      return 1;
    }
    return Math.max(
      1,
      ...dashboard.departments.map((department) => department.matchParticipationsInRange)
    );
  }, [dashboard]);

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Admin Intelligence</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-semibold text-oa-text">Company Insights Dashboard</h1>
          {dashboard ? (
            <span className="rounded-full border border-oa-border bg-black/20 px-3 py-1 text-xs text-oa-muted">
              Updated {formatDateTime(dashboard.generatedAt)}
            </span>
          ) : null}
        </div>
        <p className="mt-2 text-sm text-oa-muted">
          Company-level analytics for participation, game usage, reputation, and moderation health.
        </p>
      </header>

      <section className="grid gap-3 rounded-2xl border border-oa-border bg-oa-surface/75 p-4 lg:grid-cols-[220px_260px_auto]">
        <select
          value={selectedRange}
          onChange={(event) => setSelectedRange(event.target.value as AnalyticsRange)}
          className="rounded-lg border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
        >
          <option value="today">Today</option>
          <option value="7d">Last 7 Days</option>
          <option value="30d">Last 30 Days</option>
          <option value="all">All Time</option>
        </select>

        <select
          value={selectedDepartmentFilter}
          onChange={(event) => setSelectedDepartmentFilter(event.target.value)}
          className="rounded-lg border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
        >
          <option value="ALL">All Departments</option>
          <option value="UNASSIGNED">Unassigned Users</option>
          {departments.map((department) => (
            <option key={department.id} value={department.id}>
              {department.displayName} ({department.code}){department.active ? "" : " - INACTIVE"}
            </option>
          ))}
        </select>

        <div className="flex items-center justify-between gap-2 rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-xs text-oa-muted">
          <span>
            Scope: <span className="text-oa-text">{toRangeLabel(selectedRange)}</span> ·{" "}
            <span className="text-oa-text">{selectedDepartmentLabel}</span>
          </span>
          <button
            type="button"
            onClick={() => {
              void refresh();
            }}
            className="rounded-md border border-oa-border bg-black/25 px-3 py-1.5 text-xs text-oa-text transition-colors hover:border-oa-accent/50"
          >
            Refresh
          </button>
        </div>
      </section>

      {departmentLoadError ? (
        <p className="rounded-lg border border-oa-danger/45 bg-oa-danger/10 px-3 py-2 text-sm text-oa-danger">
          {departmentLoadError}
        </p>
      ) : null}

      {errorMessage ? (
        <p className="rounded-lg border border-oa-danger/45 bg-oa-danger/10 px-3 py-2 text-sm text-oa-danger">
          {errorMessage}
        </p>
      ) : null}

      {isLoading ? (
        <div className="space-y-4 animate-pulse">
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            {Array.from({ length: 8 }).map((_, index) => (
              <div key={index} className="h-24 rounded-xl border border-oa-border bg-oa-surface-soft/65" />
            ))}
          </div>
          <div className="h-56 rounded-2xl border border-oa-border bg-oa-surface/70" />
          <div className="h-72 rounded-2xl border border-oa-border bg-oa-surface/70" />
        </div>
      ) : null}

      {!isLoading && dashboard ? (
        <div className="space-y-5">
          <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <MetricCard
              label="Active Users (Range)"
              value={dashboard.summary.activeUsersInRange.toLocaleString()}
              helperText={`Today: ${dashboard.summary.activeUsersToday.toLocaleString()}`}
            />
            <MetricCard
              label="Matches (Range)"
              value={dashboard.summary.matchesInRange.toLocaleString()}
              helperText={`Today: ${dashboard.summary.matchesToday.toLocaleString()}`}
            />
            <MetricCard
              label="Rooms Created"
              value={dashboard.summary.roomsCreatedInRange.toLocaleString()}
              helperText={`${dashboard.filters.rangeLabel} scope`}
            />
            <MetricCard
              label="Avg Matches / Active User"
              value={dashboard.summary.averageMatchesPerActiveUser.toFixed(2)}
              helperText="Range scoped"
            />
            <MetricCard
              label="Respect Awarded"
              value={dashboard.summary.respectAwardedInRange.toLocaleString()}
              helperText="Challenge confirmations"
            />
            <MetricCard
              label="Karma Applied"
              value={dashboard.summary.karmaAppliedInRange.toLocaleString()}
              helperText="Challenge rejections"
            />
            <MetricCard
              label="Open Moderation Reports"
              value={dashboard.summary.openModerationReports.toLocaleString()}
              helperText="OPEN + IN_REVIEW"
            />
            <MetricCard
              label="Suspended Users"
              value={dashboard.summary.suspendedUsers.toLocaleString()}
              helperText={`Departments: ${dashboard.summary.totalDepartments.toLocaleString()}`}
            />
          </section>

          <section className="rounded-2xl border border-oa-border bg-oa-surface/78 p-5">
            <h2 className="text-lg font-semibold text-oa-text">Participation Trend</h2>
            <p className="mt-1 text-sm text-oa-muted">{dashboard.activityTrendLabel}</p>

            <div className="mt-4 grid gap-3 lg:grid-cols-3">
              <LineTrendCard
                title="Active Users"
                colorClass="stroke-emerald-300"
                points={dashboard.activityTrend}
                valueSelector={(point) => point.activeUsers}
              />
              <LineTrendCard
                title="Matches Played"
                colorClass="stroke-sky-300"
                points={dashboard.activityTrend}
                valueSelector={(point) => point.matchesPlayed}
              />
              <LineTrendCard
                title="Rooms Created"
                colorClass="stroke-amber-300"
                points={dashboard.activityTrend}
                valueSelector={(point) => point.roomsCreated}
              />
            </div>
          </section>

          <section className="grid gap-4 xl:grid-cols-2">
            <article className="rounded-2xl border border-oa-border bg-oa-surface/78 p-5">
              <h2 className="text-lg font-semibold text-oa-text">Game Usage</h2>
              <p className="mt-1 text-sm text-oa-muted">
                Completed matches split by game type in the selected scope.
              </p>

              {dashboard.gameUsage.length === 0 ? (
                <p className="mt-4 rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-muted">
                  No completed matches available for this scope.
                </p>
              ) : (
                <div className="mt-4 space-y-3">
                  {dashboard.gameUsage.map((item) => (
                    <div key={item.gameTypeCode} className="space-y-1">
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-medium text-oa-text">{item.gameTypeDisplayName}</span>
                        <span className="text-oa-muted">
                          {item.matchesPlayed.toLocaleString()} ({item.percentOfMatches.toFixed(1)}%)
                        </span>
                      </div>
                      <div className="h-2 overflow-hidden rounded-full border border-oa-border bg-black/30">
                        <div
                          className="h-full rounded-full bg-gradient-to-r from-oa-accent/65 to-cyan-300/70"
                          style={{ width: `${Math.max(2, item.percentOfMatches)}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}

              <p className="mt-4 text-xs text-oa-muted">
                Most played game:{" "}
                <span className="text-oa-text">
                  {topGame ? `${topGame.gameTypeDisplayName} (${topGame.matchesPlayed})` : "No data"}
                </span>
              </p>
            </article>

            <article className="rounded-2xl border border-oa-border bg-oa-surface/78 p-5">
              <h2 className="text-lg font-semibold text-oa-text">Department Participation</h2>
              <p className="mt-1 text-sm text-oa-muted">
                Top participating department: {dashboard.summary.topDepartmentByParticipation}
              </p>

              {dashboard.departments.length === 0 ? (
                <p className="mt-4 rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-muted">
                  No department analytics available for this scope.
                </p>
              ) : (
                <div className="mt-4 space-y-3">
                  {dashboard.departments.map((department) => (
                    <div key={department.departmentId ?? "UNASSIGNED"} className="space-y-1">
                      <div className="flex items-center justify-between text-sm">
                        <span className="font-medium text-oa-text">{toDepartmentLabel(department)}</span>
                        <span className="text-oa-muted">
                          {department.matchParticipationsInRange.toLocaleString()} participations
                        </span>
                      </div>
                      <div className="h-2 overflow-hidden rounded-full border border-oa-border bg-black/30">
                        <div
                          className="h-full rounded-full bg-gradient-to-r from-emerald-400/70 to-sky-400/70"
                          style={{
                            width: `${Math.max(
                              2,
                              (department.matchParticipationsInRange / maxDepartmentParticipation) * 100
                            )}%`
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </article>
          </section>

          <section className="rounded-2xl border border-oa-border bg-oa-surface/78 p-5">
            <h2 className="text-lg font-semibold text-oa-text">Department Comparison Table</h2>
            <div className="mt-4 overflow-x-auto rounded-xl border border-oa-border">
              <table className="min-w-full divide-y divide-oa-border text-sm">
                <thead className="bg-black/25 text-left text-xs uppercase tracking-[0.12em] text-oa-muted">
                  <tr>
                    <th className="px-3 py-2">Department</th>
                    <th className="px-3 py-2">Users</th>
                    <th className="px-3 py-2">Active</th>
                    <th className="px-3 py-2">Participations</th>
                    <th className="px-3 py-2">Avg Respect</th>
                    <th className="px-3 py-2">Avg Karma</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-oa-border/75">
                  {dashboard.departments.map((department) => (
                    <tr key={department.departmentId ?? "UNASSIGNED"} className="hover:bg-black/20">
                      <td className="px-3 py-2 text-oa-text">{toDepartmentLabel(department)}</td>
                      <td className="px-3 py-2 text-oa-muted">{department.userCount.toLocaleString()}</td>
                      <td className="px-3 py-2 text-oa-muted">{department.activeUsersInRange.toLocaleString()}</td>
                      <td className="px-3 py-2 text-oa-muted">
                        {department.matchParticipationsInRange.toLocaleString()}
                      </td>
                      <td className="px-3 py-2 text-oa-muted">{department.averageRespect.toFixed(1)}</td>
                      <td className="px-3 py-2 text-oa-muted">{department.averageKarma.toFixed(1)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className="grid gap-4 xl:grid-cols-2">
            <article className="rounded-2xl border border-oa-border bg-oa-surface/78 p-5">
              <h2 className="text-lg font-semibold text-oa-text">Reputation Health</h2>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <MetricCard
                  label="Challenges Created"
                  value={dashboard.reputation.challengesCreatedInRange.toLocaleString()}
                  helperText={dashboard.filters.rangeLabel}
                />
                <MetricCard
                  label="Pending Challenges"
                  value={dashboard.reputation.challengesPending.toLocaleString()}
                  helperText="Current unresolved"
                />
                <MetricCard
                  label="Disputed Challenges"
                  value={dashboard.reputation.challengesDisputed.toLocaleString()}
                  helperText="Current disputed state"
                />
                <MetricCard
                  label="Confirmed vs Rejected"
                  value={`${dashboard.reputation.confirmedChallengesInRange} / ${dashboard.reputation.rejectedChallengesInRange}`}
                  helperText="Range outcomes"
                />
              </div>
            </article>

            <article className="rounded-2xl border border-oa-border bg-oa-surface/78 p-5">
              <h2 className="text-lg font-semibold text-oa-text">Moderation & Safety</h2>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <MetricCard
                  label="Open Reports"
                  value={dashboard.moderation.openReports.toLocaleString()}
                  helperText="Status OPEN"
                />
                <MetricCard
                  label="In Review"
                  value={dashboard.moderation.inReviewReports.toLocaleString()}
                  helperText="Status IN_REVIEW"
                />
                <MetricCard
                  label="Resolved / Dismissed"
                  value={`${dashboard.moderation.resolvedReports} / ${dashboard.moderation.dismissedReports}`}
                  helperText="Current totals"
                />
                <MetricCard
                  label="Reports Created"
                  value={dashboard.moderation.reportsCreatedInRange.toLocaleString()}
                  helperText={dashboard.filters.rangeLabel}
                />
              </div>

              <div className="mt-4">
                <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Report Categories (Range)</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  {dashboard.moderation.reportCategoriesInRange.length === 0 ? (
                    <span className="text-xs text-oa-muted">No category activity in this scope.</span>
                  ) : (
                    dashboard.moderation.reportCategoriesInRange.map((item) => (
                      <span
                        key={item.category}
                        className="rounded-full border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-text"
                      >
                        {item.category}: {item.count}
                      </span>
                    ))
                  )}
                </div>
              </div>
            </article>
          </section>
        </div>
      ) : null}
    </section>
  );
}
