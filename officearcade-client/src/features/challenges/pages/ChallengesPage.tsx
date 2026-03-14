import { useMemo, useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import { PageHero } from "../../layout/PageHero";
import { ReportUserModal } from "../../moderation/components/ReportUserModal";
import { ModerationApiError, submitModerationReport } from "../../moderation/api/moderationApi";
import type { CreateModerationReportRequest } from "../../moderation/types/moderation.types";
import { useChallenges } from "../hooks/useChallenges";
import type { ChallengeSummary } from "../types/challenges.types";

function formatDateTime(value: string | null) {
  if (!value) {
    return "Not resolved";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

function statusBadgeClass(status: ChallengeSummary["status"]) {
  if (status === "PENDING") {
    return "oa-chip-warning";
  }
  if (status === "DISPUTED") {
    return "oa-chip-info";
  }
  if (status === "COMPLETED_CONFIRMED") {
    return "oa-chip-success";
  }
  if (status === "REJECTED") {
    return "oa-chip-danger";
  }
  return "";
}

function roleBadgeClass(role: ChallengeSummary["myRole"]) {
  if (role === "BENEFICIARY") {
    return "oa-chip oa-chip-success";
  }
  if (role === "OBLIGATED") {
    return "oa-chip oa-chip-warning";
  }
  return "oa-chip oa-chip-info";
}

function roleLabel(role: ChallengeSummary["myRole"]) {
  if (role === "BENEFICIARY") {
    return "Confirmer";
  }
  if (role === "OBLIGATED") {
    return "Obligated";
  }
  return "Admin Review";
}

export function ChallengesPage() {
  const { accessToken, logout, user } = useAuth();
  const { data, isLoading, isMutating, errorMessage, actionMessage, refresh, confirm, reject, dispute, clearActionMessage } =
    useChallenges(accessToken, logout);
  const [disputeNotes, setDisputeNotes] = useState<Record<string, string>>({});
  const [reportError, setReportError] = useState<string | null>(null);
  const [isReporting, setIsReporting] = useState(false);
  const [reportTarget, setReportTarget] = useState<{
    reportedUserId: string;
    reportedDisplayName: string;
    sourceRoomId: string;
    sourceGameSessionId: string;
    sourceChallengeId: string;
  } | null>(null);

  const pending = useMemo(
    () => (data?.challenges ?? []).filter((challenge) => challenge.status === "PENDING" || challenge.status === "DISPUTED"),
    [data?.challenges]
  );
  const history = useMemo(
    () => (data?.challenges ?? []).filter((challenge) => challenge.status !== "PENDING" && challenge.status !== "DISPUTED"),
    [data?.challenges]
  );

  if (!user) {
    return null;
  }

  async function handleSubmitChallengeReport(request: CreateModerationReportRequest) {
    if (!accessToken || !reportTarget) {
      return;
    }
    setIsReporting(true);
    setReportError(null);
    try {
      await submitModerationReport(accessToken, request);
      setReportTarget(null);
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setReportError(error instanceof Error ? error.message : "Unable to submit report.");
    } finally {
      setIsReporting(false);
    }
  }

  return (
    <section className="oa-page">
      <PageHero
        kicker="Reputation Layer"
        title="Respect & Karma Challenges"
        subtitle="Track obligations, confirm outcomes, and escalate disputes when needed."
        footerSlot={
          <>
            <span className="oa-chip">Total {data?.totalCount ?? 0}</span>
            <span className="oa-chip oa-chip-warning">Pending {data?.pendingCount ?? 0}</span>
            <span className="oa-chip oa-chip-success">Resolved {data?.resolvedCount ?? 0}</span>
          </>
        }
      />

      {errorMessage ? (
        <div className="oa-alert oa-alert-danger">
          {errorMessage}
        </div>
      ) : null}

      {actionMessage ? (
        <div className="oa-alert oa-alert-success flex items-center justify-between gap-3">
          <p className="text-sm text-oa-text">{actionMessage}</p>
          <button
            type="button"
            onClick={clearActionMessage}
            className="oa-btn oa-btn-ghost px-2.5 py-1 text-xs"
          >
            Dismiss
          </button>
        </div>
      ) : null}

      <section className="oa-panel">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-lg font-semibold text-oa-text">Pending Challenges</h2>
          <button
            type="button"
            onClick={() => {
              void refresh();
            }}
            className="oa-btn oa-btn-secondary px-3 py-1.5 text-xs"
            disabled={isLoading || isMutating}
          >
            {isLoading ? "Refreshing..." : "Refresh"}
          </button>
        </div>

        <div className="mt-3 space-y-3">
          {pending.length === 0 ? (
            <p className="oa-empty-state">
              No pending challenges for your account right now.
            </p>
          ) : (
            pending.map((challenge) => (
              <article key={challenge.id} className="oa-challenge-card">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h3 className="text-base font-semibold text-oa-text">{challenge.challengeTypeDisplayName}</h3>
                    <p className="mt-1 text-xs text-oa-muted">{challenge.challengeTypeDescription}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-2">
                    <span className={roleBadgeClass(challenge.myRole)}>{roleLabel(challenge.myRole)}</span>
                    <span className={`oa-chip ${statusBadgeClass(challenge.status)}`}>{challenge.status}</span>
                  </div>
                </div>

                <div className="oa-pvp-strip mt-3">
                  <div>
                    <p className="text-[10px] uppercase tracking-[0.12em] text-oa-muted">Obligated</p>
                    <p className="text-sm font-semibold text-oa-text">{challenge.obligatedDisplayName}</p>
                  </div>
                  <div className="flex items-center justify-center text-[10px] uppercase tracking-[0.18em] text-oa-muted">vs</div>
                  <div className="text-right">
                    <p className="text-[10px] uppercase tracking-[0.12em] text-oa-muted">Beneficiary</p>
                    <p className="text-sm font-semibold text-oa-text">{challenge.beneficiaryDisplayName}</p>
                  </div>
                </div>

                <div className="mt-3 grid gap-2 text-xs text-oa-muted sm:grid-cols-3">
                  <p>
                    Created <span className="text-oa-text">{formatDateTime(challenge.createdAt)}</span>
                  </p>
                  <p>
                    Respect <span className="text-oa-text">{challenge.respectPointsAwarded}</span>
                  </p>
                  <p>
                    Karma <span className="text-oa-text">{challenge.karmaPointsAwarded}</span>
                  </p>
                  {challenge.disputedAt ? <p>Disputed {formatDateTime(challenge.disputedAt)}</p> : null}
                </div>

                {challenge.disputeNote ? (
                  <p className="mt-2 rounded-lg border border-sky-300/35 bg-sky-300/10 px-3 py-2 text-xs text-sky-100">
                    Dispute note: {challenge.disputeNote}
                  </p>
                ) : null}
                {challenge.resolutionNote ? (
                  <p className="mt-2 rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-xs text-oa-text">
                    Resolution note: {challenge.resolutionNote}
                  </p>
                ) : null}

                {challenge.status === "PENDING" && challenge.canResolve ? (
                  <div className="mt-3 flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        void confirm(challenge.id);
                      }}
                      className="oa-btn oa-btn-primary px-3 py-1.5 text-xs"
                      disabled={isMutating}
                    >
                      Confirm Completed (+Respect)
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        void reject(challenge.id);
                      }}
                      className="oa-btn oa-btn-danger px-3 py-1.5 text-xs"
                      disabled={isMutating}
                    >
                      Reject / Not Fulfilled (+Karma)
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        setReportTarget({
                          reportedUserId: challenge.obligatedUserId,
                          reportedDisplayName: challenge.obligatedDisplayName,
                          sourceRoomId: challenge.sourceRoomId,
                          sourceGameSessionId: challenge.sourceGameSessionId,
                          sourceChallengeId: challenge.id
                        });
                      }}
                      className="oa-btn oa-btn-ghost px-3 py-1.5 text-xs"
                      disabled={isMutating}
                    >
                      Report Counterparty
                    </button>
                  </div>
                ) : challenge.status === "PENDING" ? (
                  <div className="mt-3 space-y-2">
                    <p className="text-xs text-oa-muted">
                      Obligated users can escalate a pending challenge for moderation review if needed.
                    </p>
                    <textarea
                      value={disputeNotes[challenge.id] ?? ""}
                      onChange={(event) =>
                        setDisputeNotes((previous) => ({
                          ...previous,
                          [challenge.id]: event.target.value.slice(0, 280)
                        }))
                      }
                      rows={2}
                      className="oa-textarea text-xs"
                      placeholder="Dispute note (optional)"
                      disabled={isMutating}
                    />
                    <div className="flex flex-wrap gap-2">
                      <button
                        type="button"
                        onClick={() => {
                          void dispute(challenge.id, disputeNotes[challenge.id] ?? "");
                        }}
                        className="oa-btn oa-btn-secondary px-3 py-1.5 text-xs"
                        disabled={isMutating}
                      >
                        Dispute for Admin Review
                      </button>
                      <button
                        type="button"
                        onClick={() => {
                          const reportedUserId =
                            challenge.myRole === "OBLIGATED" ? challenge.beneficiaryUserId : challenge.obligatedUserId;
                          const reportedDisplayName =
                            challenge.myRole === "OBLIGATED"
                              ? challenge.beneficiaryDisplayName
                              : challenge.obligatedDisplayName;
                          setReportTarget({
                            reportedUserId,
                            reportedDisplayName,
                            sourceRoomId: challenge.sourceRoomId,
                            sourceGameSessionId: challenge.sourceGameSessionId,
                            sourceChallengeId: challenge.id
                          });
                        }}
                        className="oa-btn oa-btn-ghost px-3 py-1.5 text-xs"
                        disabled={isMutating}
                      >
                        Report Counterparty
                      </button>
                    </div>
                  </div>
                ) : (
                  <p className="mt-3 text-xs text-oa-muted">
                    This challenge is currently in moderation review and cannot be resolved by participants.
                  </p>
                )}
              </article>
            ))
          )}
        </div>
      </section>

      <section className="oa-panel">
        <h2 className="text-lg font-semibold text-oa-text">Resolved History</h2>
        <div className="mt-3 space-y-3">
          {history.length === 0 ? (
            <p className="oa-empty-state">
              No resolved challenges yet.
            </p>
          ) : (
            history.map((challenge) => (
              <article key={challenge.id} className="oa-room-card">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm font-medium text-oa-text">{challenge.challengeTypeDisplayName}</p>
                  <span className={`oa-chip ${statusBadgeClass(challenge.status)}`}>
                    {challenge.status}
                  </span>
                </div>
                <div className="oa-pvp-strip mt-2">
                  <div>
                    <p className="text-[10px] uppercase tracking-[0.12em] text-oa-muted">Obligated</p>
                    <p className="text-xs text-oa-text">{challenge.obligatedDisplayName}</p>
                  </div>
                  <div className="flex items-center justify-center text-[10px] uppercase tracking-[0.16em] text-oa-muted">vs</div>
                  <div className="text-right">
                    <p className="text-[10px] uppercase tracking-[0.12em] text-oa-muted">Beneficiary</p>
                    <p className="text-xs text-oa-text">{challenge.beneficiaryDisplayName}</p>
                  </div>
                </div>
                <div className="mt-2 grid gap-2 text-xs text-oa-muted sm:grid-cols-4">
                  <p>Resolved {formatDateTime(challenge.resolvedAt)}</p>
                  <p>Respect Applied: {challenge.respectPointsAwarded}</p>
                  <p>Karma Applied: {challenge.karmaPointsAwarded}</p>
                  <p>Role: {roleLabel(challenge.myRole)}</p>
                </div>
                <div className="mt-2">
                  <button
                    type="button"
                    onClick={() => {
                      const reportedUserId =
                        challenge.myRole === "OBLIGATED" ? challenge.beneficiaryUserId : challenge.obligatedUserId;
                      const reportedDisplayName =
                        challenge.myRole === "OBLIGATED" ? challenge.beneficiaryDisplayName : challenge.obligatedDisplayName;
                      setReportTarget({
                        reportedUserId,
                        reportedDisplayName,
                        sourceRoomId: challenge.sourceRoomId,
                        sourceGameSessionId: challenge.sourceGameSessionId,
                        sourceChallengeId: challenge.id
                      });
                    }}
                    className="oa-btn oa-btn-ghost px-3 py-1.5 text-xs"
                  >
                    Report Counterparty
                  </button>
                </div>
              </article>
            ))
          )}
        </div>
      </section>

      <ReportUserModal
        isOpen={reportTarget !== null}
        reportedUserId={reportTarget?.reportedUserId ?? ""}
        reportedDisplayName={reportTarget?.reportedDisplayName ?? ""}
        context={
          reportTarget
            ? {
                sourceRoomId: reportTarget.sourceRoomId,
                sourceGameSessionId: reportTarget.sourceGameSessionId,
                sourceChallengeId: reportTarget.sourceChallengeId,
                defaultCategory: "CHALLENGE_DISPUTE"
              }
            : undefined
        }
        isSubmitting={isReporting}
        errorMessage={reportError}
        onClose={() => {
          if (isReporting) {
            return;
          }
          setReportError(null);
          setReportTarget(null);
        }}
        onSubmit={handleSubmitChallengeReport}
      />
    </section>
  );
}
