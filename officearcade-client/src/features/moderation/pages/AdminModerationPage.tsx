import { useCallback, useEffect, useMemo, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import {
  dismissAdminModerationReport,
  getAdminModerationReportById,
  listAdminDisputedChallenges,
  listAdminModerationReports,
  listChallengePolicies,
  listModerationAuditEntries,
  ModerationApiError,
  resolveAdminDisputedChallenge,
  takeAdminModerationReportAction,
  updateChallengePolicies
} from "../api/moderationApi";
import type {
  AdminChallengeReview,
  ChallengeDisputeResolutionType,
  ChallengePolicySetting,
  ModerationAuditEntry,
  ModerationReport,
  ModerationReportActionType,
  ModerationReportCategory,
  ModerationReportStatus
} from "../types/moderation.types";

type StatusFilter = "QUEUE" | ModerationReportStatus;
type CategoryFilter = "ALL" | ModerationReportCategory;

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

function statusBadgeClass(status: ModerationReportStatus) {
  if (status === "OPEN") {
    return "border-amber-300/45 bg-amber-300/15 text-amber-100";
  }
  if (status === "IN_REVIEW") {
    return "border-sky-300/45 bg-sky-300/15 text-sky-100";
  }
  if (status === "RESOLVED") {
    return "border-oa-accent/45 bg-oa-accent/15 text-oa-text";
  }
  return "border-oa-danger/45 bg-oa-danger/15 text-oa-danger";
}

function categoryLabel(category: ModerationReportCategory) {
  if (category === "UNSPORTSMANLIKE_BEHAVIOR") {
    return "Unsportsmanlike";
  }
  if (category === "CHALLENGE_DISPUTE") {
    return "Challenge Dispute";
  }
  if (category === "HARASSMENT_OR_INAPPROPRIATE_BEHAVIOR") {
    return "Harassment / Inappropriate";
  }
  if (category === "ABUSE_OF_SYSTEM") {
    return "Abuse of System";
  }
  return "Other";
}

function toErrorMessage(error: unknown, fallback: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallback;
}

export function AdminModerationPage() {
  const { accessToken, logout } = useAuth();

  const [statusFilter, setStatusFilter] = useState<StatusFilter>("QUEUE");
  const [categoryFilter, setCategoryFilter] = useState<CategoryFilter>("ALL");

  const [queueReports, setQueueReports] = useState<ModerationReport[]>([]);
  const [selectedReportId, setSelectedReportId] = useState<string | null>(null);
  const [selectedReport, setSelectedReport] = useState<ModerationReport | null>(null);
  const [reportActionNote, setReportActionNote] = useState("");

  const [isLoadingQueue, setIsLoadingQueue] = useState(true);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [isApplyingAction, setIsApplyingAction] = useState(false);
  const [queueError, setQueueError] = useState<string | null>(null);

  const [disputedChallenges, setDisputedChallenges] = useState<AdminChallengeReview[]>([]);
  const [challengeNotes, setChallengeNotes] = useState<Record<string, string>>({});
  const [isLoadingChallenges, setIsLoadingChallenges] = useState(true);
  const [isResolvingChallengeId, setIsResolvingChallengeId] = useState<string | null>(null);

  const [auditEntries, setAuditEntries] = useState<ModerationAuditEntry[]>([]);
  const [isLoadingAudit, setIsLoadingAudit] = useState(true);

  const [policies, setPolicies] = useState<ChallengePolicySetting[]>([]);
  const [isLoadingPolicies, setIsLoadingPolicies] = useState(true);
  const [isSavingPolicy, setIsSavingPolicy] = useState(false);

  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);
  const [feedbackError, setFeedbackError] = useState<string | null>(null);

  const loadReports = useCallback(async () => {
    if (!accessToken) {
      return;
    }

    setIsLoadingQueue(true);
    setQueueError(null);
    try {
      const response = await listAdminModerationReports(accessToken, {
        status: statusFilter === "QUEUE" ? undefined : statusFilter,
        category: categoryFilter === "ALL" ? undefined : categoryFilter
      });
      setQueueReports(response.reports);
      if (response.reports.length === 0) {
        setSelectedReportId(null);
        setSelectedReport(null);
      } else if (!selectedReportId || !response.reports.some((report) => report.id === selectedReportId)) {
        setSelectedReportId(response.reports[0].id);
      }
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setQueueError(toErrorMessage(error, "Unable to load moderation queue."));
    } finally {
      setIsLoadingQueue(false);
    }
  }, [accessToken, categoryFilter, logout, selectedReportId, statusFilter]);

  const loadDisputedChallenges = useCallback(async () => {
    if (!accessToken) {
      return;
    }
    setIsLoadingChallenges(true);
    try {
      const response = await listAdminDisputedChallenges(accessToken);
      setDisputedChallenges(response.challenges);
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setFeedbackError(toErrorMessage(error, "Unable to load disputed challenges."));
    } finally {
      setIsLoadingChallenges(false);
    }
  }, [accessToken, logout]);

  const loadAudit = useCallback(async () => {
    if (!accessToken) {
      return;
    }
    setIsLoadingAudit(true);
    try {
      const response = await listModerationAuditEntries(accessToken);
      setAuditEntries(response.entries);
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setFeedbackError(toErrorMessage(error, "Unable to load audit history."));
    } finally {
      setIsLoadingAudit(false);
    }
  }, [accessToken, logout]);

  const loadPolicies = useCallback(async () => {
    if (!accessToken) {
      return;
    }
    setIsLoadingPolicies(true);
    try {
      const response = await listChallengePolicies(accessToken);
      setPolicies(response.policies);
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setFeedbackError(toErrorMessage(error, "Unable to load challenge policies."));
    } finally {
      setIsLoadingPolicies(false);
    }
  }, [accessToken, logout]);

  useEffect(() => {
    void loadReports();
  }, [loadReports]);

  useEffect(() => {
    void Promise.all([loadDisputedChallenges(), loadAudit(), loadPolicies()]);
  }, [loadAudit, loadDisputedChallenges, loadPolicies]);

  useEffect(() => {
    async function loadReportDetail() {
      if (!accessToken || !selectedReportId) {
        setSelectedReport(null);
        return;
      }
      setIsLoadingDetail(true);
      try {
        const detail = await getAdminModerationReportById(accessToken, selectedReportId);
        setSelectedReport(detail);
      } catch (error) {
        if (error instanceof ModerationApiError && error.status === 401) {
          logout();
          return;
        }
        setFeedbackError(toErrorMessage(error, "Unable to load report details."));
      } finally {
        setIsLoadingDetail(false);
      }
    }

    void loadReportDetail();
  }, [accessToken, logout, selectedReportId]);

  const unresolvedCount = useMemo(
    () => queueReports.filter((report) => report.status === "OPEN" || report.status === "IN_REVIEW").length,
    [queueReports]
  );

  async function handleReportAction(actionType: ModerationReportActionType) {
    if (!accessToken || !selectedReport) {
      return;
    }
    setIsApplyingAction(true);
    setFeedbackError(null);
    setFeedbackMessage(null);

    try {
      const updated = await takeAdminModerationReportAction(accessToken, selectedReport.id, actionType, reportActionNote);
      setSelectedReport(updated);
      setReportActionNote("");
      setFeedbackMessage(`Applied ${actionType} on report ${updated.id}.`);
      await Promise.all([loadReports(), loadAudit()]);
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setFeedbackError(toErrorMessage(error, "Unable to apply moderation action."));
    } finally {
      setIsApplyingAction(false);
    }
  }

  async function handleDismissReport() {
    if (!accessToken || !selectedReport) {
      return;
    }
    setIsApplyingAction(true);
    setFeedbackError(null);
    setFeedbackMessage(null);

    try {
      const updated = await dismissAdminModerationReport(accessToken, selectedReport.id, reportActionNote);
      setSelectedReport(updated);
      setReportActionNote("");
      setFeedbackMessage(`Dismissed report ${updated.id}.`);
      await Promise.all([loadReports(), loadAudit()]);
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setFeedbackError(toErrorMessage(error, "Unable to dismiss report."));
    } finally {
      setIsApplyingAction(false);
    }
  }

  async function handleResolveChallenge(challengeId: string, resolutionType: ChallengeDisputeResolutionType) {
    if (!accessToken) {
      return;
    }
    setIsResolvingChallengeId(challengeId);
    setFeedbackError(null);
    setFeedbackMessage(null);

    try {
      await resolveAdminDisputedChallenge(accessToken, challengeId, resolutionType, challengeNotes[challengeId] ?? "");
      setFeedbackMessage(`Resolved disputed challenge ${challengeId}.`);
      setChallengeNotes((previous) => ({ ...previous, [challengeId]: "" }));
      await Promise.all([loadDisputedChallenges(), loadAudit()]);
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setFeedbackError(toErrorMessage(error, "Unable to resolve challenge dispute."));
    } finally {
      setIsResolvingChallengeId(null);
    }
  }

  async function handlePolicyToggle(code: string, enabled: boolean) {
    if (!accessToken) {
      return;
    }
    const nextPolicies = policies.map((policy) => (policy.code === code ? { ...policy, enabled } : policy));
    setPolicies(nextPolicies);
    setIsSavingPolicy(true);
    setFeedbackError(null);

    try {
      const response = await updateChallengePolicies(accessToken, {
        policies: nextPolicies.map((policy) => ({ code: policy.code, enabled: policy.enabled }))
      });
      setPolicies(response.policies);
      setFeedbackMessage("Challenge policy settings updated.");
      await loadAudit();
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setFeedbackError(toErrorMessage(error, "Unable to update challenge policy settings."));
      await loadPolicies();
    } finally {
      setIsSavingPolicy(false);
    }
  }

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Admin Safety Layer</p>
        <h1 className="mt-2 text-2xl font-semibold text-oa-text">Moderation and Workplace Controls</h1>
        <p className="mt-2 text-sm text-oa-muted">
          Review reports, resolve challenge disputes, suspend accounts when required, and maintain an audit trail.
        </p>
        <div className="mt-3 flex flex-wrap gap-2 text-xs">
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-oa-text">
            Queue Items: {queueReports.length}
          </span>
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-oa-text">
            Unresolved: {unresolvedCount}
          </span>
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-oa-text">
            Disputed Challenges: {disputedChallenges.length}
          </span>
        </div>
      </header>

      {feedbackMessage ? (
        <div className="rounded-xl border border-oa-accent/45 bg-oa-accent/10 px-4 py-3 text-sm text-oa-text">{feedbackMessage}</div>
      ) : null}
      {feedbackError ? (
        <div className="rounded-xl border border-oa-danger/45 bg-oa-danger/10 px-4 py-3 text-sm text-oa-danger">{feedbackError}</div>
      ) : null}

      <section className="grid gap-4 xl:grid-cols-[1.1fr_0.9fr]">
        <article className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-oa-text">Moderation Queue</h2>
            <button
              type="button"
              onClick={() => {
                void loadReports();
              }}
              className="rounded-md border border-oa-border bg-black/20 px-3 py-1.5 text-xs text-oa-text transition-colors hover:border-oa-accent/45"
              disabled={isLoadingQueue}
            >
              {isLoadingQueue ? "Refreshing..." : "Refresh"}
            </button>
          </div>

          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            <select
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value as StatusFilter)}
              className="rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
            >
              <option value="QUEUE">Queue (Open + In Review)</option>
              <option value="OPEN">OPEN</option>
              <option value="IN_REVIEW">IN_REVIEW</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="DISMISSED">DISMISSED</option>
            </select>

            <select
              value={categoryFilter}
              onChange={(event) => setCategoryFilter(event.target.value as CategoryFilter)}
              className="rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
            >
              <option value="ALL">All Categories</option>
              <option value="UNSPORTSMANLIKE_BEHAVIOR">Unsportsmanlike</option>
              <option value="CHALLENGE_DISPUTE">Challenge Dispute</option>
              <option value="HARASSMENT_OR_INAPPROPRIATE_BEHAVIOR">Harassment / Inappropriate</option>
              <option value="ABUSE_OF_SYSTEM">Abuse of System</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          {queueError ? <p className="mt-3 text-sm text-oa-danger">{queueError}</p> : null}

          <div className="mt-3 overflow-x-auto rounded-xl border border-oa-border">
            <table className="min-w-full divide-y divide-oa-border text-sm">
              <thead className="bg-black/25 text-left text-xs uppercase tracking-[0.12em] text-oa-muted">
                <tr>
                  <th className="px-3 py-2">Status</th>
                  <th className="px-3 py-2">Reported User</th>
                  <th className="px-3 py-2">Category</th>
                  <th className="px-3 py-2">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-oa-border/80">
                {isLoadingQueue ? (
                  <tr>
                    <td colSpan={4} className="px-3 py-5 text-center text-oa-muted">
                      Loading moderation queue...
                    </td>
                  </tr>
                ) : queueReports.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="px-3 py-5 text-center text-oa-muted">
                      No reports match current filters.
                    </td>
                  </tr>
                ) : (
                  queueReports.map((report) => (
                    <tr
                      key={report.id}
                      className={`cursor-pointer transition-colors hover:bg-black/25 ${
                        selectedReportId === report.id ? "bg-oa-accent/10" : ""
                      }`}
                      onClick={() => setSelectedReportId(report.id)}
                    >
                      <td className="px-3 py-2">
                        <span className={`rounded-full border px-2 py-0.5 text-xs ${statusBadgeClass(report.status)}`}>
                          {report.status}
                        </span>
                      </td>
                      <td className="px-3 py-2 text-oa-text">{report.reportedDisplayName}</td>
                      <td className="px-3 py-2 text-oa-muted">{categoryLabel(report.category)}</td>
                      <td className="px-3 py-2 text-oa-muted">{formatDateTime(report.createdAt)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </article>

        <article className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
          <h2 className="text-lg font-semibold text-oa-text">Report Detail</h2>
          {isLoadingDetail ? (
            <p className="mt-3 text-sm text-oa-muted">Loading selected report...</p>
          ) : !selectedReport ? (
            <p className="mt-3 text-sm text-oa-muted">Select a report from the queue.</p>
          ) : (
            <div className="mt-3 space-y-3">
              <div className="rounded-xl border border-oa-border bg-black/20 p-3 text-sm">
                <p>
                  <span className="text-oa-muted">Reporter:</span> <span className="text-oa-text">{selectedReport.reporterDisplayName}</span>
                </p>
                <p>
                  <span className="text-oa-muted">Reported:</span> <span className="text-oa-text">{selectedReport.reportedDisplayName}</span>
                </p>
                <p>
                  <span className="text-oa-muted">Category:</span> <span className="text-oa-text">{categoryLabel(selectedReport.category)}</span>
                </p>
                <p>
                  <span className="text-oa-muted">Status:</span>{" "}
                  <span className={`rounded-full border px-2 py-0.5 text-xs ${statusBadgeClass(selectedReport.status)}`}>
                    {selectedReport.status}
                  </span>
                </p>
                <p>
                  <span className="text-oa-muted">Created:</span> <span className="text-oa-text">{formatDateTime(selectedReport.createdAt)}</span>
                </p>
                <p>
                  <span className="text-oa-muted">Context:</span>{" "}
                  <span className="text-oa-text">
                    room={selectedReport.sourceRoomId ?? "-"}, game={selectedReport.sourceGameSessionId ?? "-"}, challenge=
                    {selectedReport.sourceChallengeId ?? "-"}
                  </span>
                </p>
                <p className="mt-2 rounded-lg border border-oa-border bg-black/30 px-2.5 py-2 text-xs text-oa-muted">
                  {selectedReport.note?.trim() ? selectedReport.note : "No report note provided."}
                </p>
                {selectedReport.resolutionNote ? (
                  <p className="mt-2 rounded-lg border border-oa-border bg-black/30 px-2.5 py-2 text-xs text-oa-text">
                    Resolution note: {selectedReport.resolutionNote}
                  </p>
                ) : null}
              </div>

              <textarea
                value={reportActionNote}
                onChange={(event) => setReportActionNote(event.target.value.slice(0, 280))}
                rows={3}
                className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                placeholder="Moderation note (optional)"
                disabled={isApplyingAction || selectedReport.status === "RESOLVED" || selectedReport.status === "DISMISSED"}
              />

              <div className="grid gap-2 sm:grid-cols-2">
                <button
                  type="button"
                  onClick={() => {
                    void handleReportAction("NOTE_ONLY");
                  }}
                  className="rounded-lg border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-65"
                  disabled={isApplyingAction || selectedReport.status === "RESOLVED" || selectedReport.status === "DISMISSED"}
                >
                  Note Only
                </button>
                <button
                  type="button"
                  onClick={() => {
                    void handleDismissReport();
                  }}
                  className="rounded-lg border border-oa-danger/45 bg-oa-danger/15 px-3 py-2 text-sm text-oa-danger transition-colors hover:bg-oa-danger/25 disabled:cursor-not-allowed disabled:opacity-65"
                  disabled={isApplyingAction || selectedReport.status === "RESOLVED" || selectedReport.status === "DISMISSED"}
                >
                  Dismiss Report
                </button>
                <button
                  type="button"
                  onClick={() => {
                    void handleReportAction("SUSPEND_USER");
                  }}
                  className="rounded-lg border border-oa-danger/45 bg-oa-danger/15 px-3 py-2 text-sm text-oa-danger transition-colors hover:bg-oa-danger/25 disabled:cursor-not-allowed disabled:opacity-65"
                  disabled={isApplyingAction || selectedReport.status === "RESOLVED" || selectedReport.status === "DISMISSED"}
                >
                  Suspend User
                </button>
                <button
                  type="button"
                  onClick={() => {
                    void handleReportAction("UNSUSPEND_USER");
                  }}
                  className="rounded-lg border border-oa-accent/45 bg-oa-accent/15 px-3 py-2 text-sm text-oa-text transition-colors hover:bg-oa-accent/25 disabled:cursor-not-allowed disabled:opacity-65"
                  disabled={isApplyingAction || selectedReport.status === "RESOLVED" || selectedReport.status === "DISMISSED"}
                >
                  Unsuspend User
                </button>
              </div>
            </div>
          )}
        </article>
      </section>

      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-lg font-semibold text-oa-text">Challenge Dispute Review</h2>
          <button
            type="button"
            onClick={() => {
              void loadDisputedChallenges();
            }}
            className="rounded-md border border-oa-border bg-black/20 px-3 py-1.5 text-xs text-oa-text transition-colors hover:border-oa-accent/45"
            disabled={isLoadingChallenges}
          >
            {isLoadingChallenges ? "Refreshing..." : "Refresh"}
          </button>
        </div>

        <div className="mt-3 space-y-3">
          {isLoadingChallenges ? (
            <p className="text-sm text-oa-muted">Loading disputed challenges...</p>
          ) : disputedChallenges.length === 0 ? (
            <p className="rounded-lg border border-oa-border bg-black/20 px-3 py-3 text-sm text-oa-muted">
              No disputed challenges currently waiting for admin review.
            </p>
          ) : (
            disputedChallenges.map((challenge) => (
              <article key={challenge.challengeId} className="rounded-xl border border-oa-border bg-black/20 p-3">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="text-sm font-semibold text-oa-text">{challenge.challengeTypeDisplayName}</h3>
                  <span className="rounded-full border border-amber-300/45 bg-amber-300/15 px-2 py-0.5 text-xs text-amber-100">
                    {challenge.status}
                  </span>
                </div>
                <p className="mt-2 text-xs text-oa-muted">
                  Obligated: {challenge.obligatedDisplayName} | Beneficiary: {challenge.beneficiaryDisplayName}
                </p>
                <p className="text-xs text-oa-muted">Disputed at: {formatDateTime(challenge.disputedAt)}</p>
                <p className="mt-2 rounded-lg border border-oa-border bg-black/30 px-2.5 py-2 text-xs text-oa-text">
                  {challenge.disputeNote?.trim() ? challenge.disputeNote : "No dispute note provided."}
                </p>
                <textarea
                  value={challengeNotes[challenge.challengeId] ?? ""}
                  onChange={(event) =>
                    setChallengeNotes((previous) => ({
                      ...previous,
                      [challenge.challengeId]: event.target.value.slice(0, 280)
                    }))
                  }
                  rows={2}
                  className="mt-2 w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-xs text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                  placeholder="Resolution note (optional)"
                  disabled={isResolvingChallengeId === challenge.challengeId}
                />
                <div className="mt-2 grid gap-2 sm:grid-cols-3">
                  <button
                    type="button"
                    onClick={() => {
                      void handleResolveChallenge(challenge.challengeId, "CONFIRM_COMPLETED");
                    }}
                    className="rounded-lg border border-oa-accent/45 bg-oa-accent/15 px-2.5 py-1.5 text-xs text-oa-text transition-colors hover:bg-oa-accent/25 disabled:cursor-not-allowed disabled:opacity-65"
                    disabled={isResolvingChallengeId === challenge.challengeId}
                  >
                    Confirm Completed
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      void handleResolveChallenge(challenge.challengeId, "REJECT_NOT_FULFILLED");
                    }}
                    className="rounded-lg border border-oa-danger/45 bg-oa-danger/15 px-2.5 py-1.5 text-xs text-oa-danger transition-colors hover:bg-oa-danger/25 disabled:cursor-not-allowed disabled:opacity-65"
                    disabled={isResolvingChallengeId === challenge.challengeId}
                  >
                    Reject (Karma)
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      void handleResolveChallenge(challenge.challengeId, "CANCEL_WITHOUT_POINTS");
                    }}
                    className="rounded-lg border border-oa-border bg-black/30 px-2.5 py-1.5 text-xs text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-65"
                    disabled={isResolvingChallengeId === challenge.challengeId}
                  >
                    Cancel (Neutral)
                  </button>
                </div>
              </article>
            ))
          )}
        </div>
      </section>

      <section className="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
        <article className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
          <h2 className="text-lg font-semibold text-oa-text">Challenge Policy Controls</h2>
          <p className="mt-1 text-xs text-oa-muted">
            Toggle safe challenge types. At least one challenge type must stay enabled.
          </p>

          <div className="mt-3 space-y-2">
            {isLoadingPolicies ? (
              <p className="text-sm text-oa-muted">Loading policy settings...</p>
            ) : (
              policies.map((policy) => (
                <label
                  key={policy.code}
                  className="flex cursor-pointer items-center justify-between rounded-lg border border-oa-border bg-black/20 px-3 py-2"
                >
                  <div>
                    <p className="text-sm font-medium text-oa-text">{policy.displayName}</p>
                    <p className="text-xs text-oa-muted">{policy.code}</p>
                  </div>
                  <input
                    type="checkbox"
                    checked={policy.enabled}
                    disabled={isSavingPolicy}
                    onChange={(event) => {
                      void handlePolicyToggle(policy.code, event.target.checked);
                    }}
                    className="h-4 w-4 rounded border-oa-border bg-black/20 text-oa-accent focus:ring-oa-accent/40"
                  />
                </label>
              ))
            )}
          </div>
        </article>

        <article className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
          <div className="flex items-center justify-between gap-2">
            <h2 className="text-lg font-semibold text-oa-text">Recent Moderation Audit</h2>
            <button
              type="button"
              onClick={() => {
                void loadAudit();
              }}
              className="rounded-md border border-oa-border bg-black/20 px-3 py-1.5 text-xs text-oa-text transition-colors hover:border-oa-accent/45"
              disabled={isLoadingAudit}
            >
              {isLoadingAudit ? "Refreshing..." : "Refresh"}
            </button>
          </div>

          <div className="mt-3 max-h-80 space-y-2 overflow-y-auto pr-1">
            {isLoadingAudit ? (
              <p className="text-sm text-oa-muted">Loading audit entries...</p>
            ) : auditEntries.length === 0 ? (
              <p className="rounded-lg border border-oa-border bg-black/20 px-3 py-3 text-sm text-oa-muted">
                No audit entries available yet.
              </p>
            ) : (
              auditEntries.map((entry) => (
                <article key={entry.id} className="rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-xs">
                  <p className="text-oa-text">
                    <span className="font-semibold">{entry.actionType}</span> by {entry.adminDisplayName}
                  </p>
                  <p className="text-oa-muted">
                    target={entry.targetDisplayName ?? "-"} | report={entry.reportId ?? "-"} | challenge={entry.challengeId ?? "-"}
                  </p>
                  <p className="text-oa-muted">{formatDateTime(entry.createdAt)}</p>
                  {entry.note ? <p className="mt-1 text-oa-text">{entry.note}</p> : null}
                </article>
              ))
            )}
          </div>
        </article>
      </section>
    </section>
  );
}
