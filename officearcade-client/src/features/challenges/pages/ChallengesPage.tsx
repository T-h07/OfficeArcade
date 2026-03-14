import { useMemo } from "react";
import { useAuth } from "../../auth/AuthContext";
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
    return "border-amber-300/45 bg-amber-300/15 text-amber-100";
  }
  if (status === "COMPLETED_CONFIRMED") {
    return "border-oa-accent/45 bg-oa-accent/15 text-oa-text";
  }
  if (status === "REJECTED") {
    return "border-oa-danger/45 bg-oa-danger/15 text-oa-danger";
  }
  return "border-oa-border bg-black/25 text-oa-muted";
}

export function ChallengesPage() {
  const { accessToken, logout, user } = useAuth();
  const { data, isLoading, isMutating, errorMessage, actionMessage, refresh, confirm, reject, clearActionMessage } =
    useChallenges(accessToken, logout);

  const pending = useMemo(
    () => (data?.challenges ?? []).filter((challenge) => challenge.status === "PENDING"),
    [data?.challenges]
  );
  const history = useMemo(
    () => (data?.challenges ?? []).filter((challenge) => challenge.status !== "PENDING"),
    [data?.challenges]
  );

  if (!user) {
    return null;
  }

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Reputation Layer</p>
        <h1 className="mt-2 text-2xl font-semibold text-oa-text">Respect & Karma Challenges</h1>
        <p className="mt-2 text-sm text-oa-muted">
          Safe post-match obligations generated from completed Connect Four results.
        </p>
      </header>

      {errorMessage ? (
        <div className="rounded-xl border border-oa-danger/45 bg-oa-danger/10 px-4 py-3 text-sm text-oa-danger">
          {errorMessage}
        </div>
      ) : null}

      {actionMessage ? (
        <div className="flex items-center justify-between gap-3 rounded-xl border border-oa-accent/45 bg-oa-accent/10 px-4 py-3">
          <p className="text-sm text-oa-text">{actionMessage}</p>
          <button
            type="button"
            onClick={clearActionMessage}
            className="rounded-md border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-muted transition-colors hover:border-oa-accent/45 hover:text-oa-text"
          >
            Dismiss
          </button>
        </div>
      ) : null}

      <section className="grid gap-3 sm:grid-cols-3">
        <article className="rounded-xl border border-oa-border bg-oa-surface/70 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Total</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{data?.totalCount ?? 0}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface/70 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Pending</p>
          <p className="mt-2 text-2xl font-semibold text-amber-100">{data?.pendingCount ?? 0}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface/70 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Resolved</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{data?.resolvedCount ?? 0}</p>
        </article>
      </section>

      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
        <div className="flex items-center justify-between gap-2">
          <h2 className="text-lg font-semibold text-oa-text">Pending Challenges</h2>
          <button
            type="button"
            onClick={() => {
              void refresh();
            }}
            className="rounded-md border border-oa-border bg-black/20 px-3 py-1.5 text-xs text-oa-text transition-colors hover:border-oa-accent/50 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isLoading || isMutating}
          >
            {isLoading ? "Refreshing..." : "Refresh"}
          </button>
        </div>

        <div className="mt-3 space-y-3">
          {pending.length === 0 ? (
            <p className="rounded-lg border border-oa-border bg-black/20 px-4 py-3 text-sm text-oa-muted">
              No pending challenges for your account right now.
            </p>
          ) : (
            pending.map((challenge) => (
              <article key={challenge.id} className="rounded-xl border border-oa-border bg-black/20 p-4">
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h3 className="text-base font-semibold text-oa-text">{challenge.challengeTypeDisplayName}</h3>
                    <p className="mt-1 text-sm text-oa-muted">{challenge.challengeTypeDescription}</p>
                  </div>
                  <span className={`rounded-full border px-2.5 py-1 text-xs ${statusBadgeClass(challenge.status)}`}>
                    {challenge.status}
                  </span>
                </div>

                <div className="mt-3 grid gap-2 text-sm text-oa-muted sm:grid-cols-2">
                  <p>
                    <span className="text-oa-text">Obligated:</span> {challenge.obligatedDisplayName}
                  </p>
                  <p>
                    <span className="text-oa-text">Beneficiary:</span> {challenge.beneficiaryDisplayName}
                  </p>
                  <p>
                    <span className="text-oa-text">Created:</span> {formatDateTime(challenge.createdAt)}
                  </p>
                  <p>
                    <span className="text-oa-text">My Role:</span> {challenge.myRole}
                  </p>
                </div>

                {challenge.canResolve ? (
                  <div className="mt-3 flex flex-wrap gap-2">
                    <button
                      type="button"
                      onClick={() => {
                        void confirm(challenge.id);
                      }}
                      className="rounded-md border border-oa-accent/50 bg-oa-accent/20 px-3 py-1.5 text-xs font-semibold text-oa-text transition-colors hover:bg-oa-accent/30 disabled:cursor-not-allowed disabled:opacity-60"
                      disabled={isMutating}
                    >
                      Confirm Completed (+Respect)
                    </button>
                    <button
                      type="button"
                      onClick={() => {
                        void reject(challenge.id);
                      }}
                      className="rounded-md border border-oa-danger/45 bg-oa-danger/15 px-3 py-1.5 text-xs font-semibold text-oa-danger transition-colors hover:bg-oa-danger/25 disabled:cursor-not-allowed disabled:opacity-60"
                      disabled={isMutating}
                    >
                      Reject / Not Fulfilled (+Karma)
                    </button>
                  </div>
                ) : (
                  <p className="mt-3 text-xs text-oa-muted">
                    Waiting for beneficiary confirmation. Obligated users cannot self-confirm in OA-PT09.
                  </p>
                )}
              </article>
            ))
          )}
        </div>
      </section>

      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
        <h2 className="text-lg font-semibold text-oa-text">Resolved History</h2>
        <div className="mt-3 space-y-3">
          {history.length === 0 ? (
            <p className="rounded-lg border border-oa-border bg-black/20 px-4 py-3 text-sm text-oa-muted">
              No resolved challenges yet.
            </p>
          ) : (
            history.map((challenge) => (
              <article key={challenge.id} className="rounded-xl border border-oa-border bg-black/20 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm font-medium text-oa-text">{challenge.challengeTypeDisplayName}</p>
                  <span className={`rounded-full border px-2.5 py-1 text-xs ${statusBadgeClass(challenge.status)}`}>
                    {challenge.status}
                  </span>
                </div>
                <div className="mt-2 grid gap-2 text-xs text-oa-muted sm:grid-cols-3">
                  <p>Obligated: {challenge.obligatedDisplayName}</p>
                  <p>Beneficiary: {challenge.beneficiaryDisplayName}</p>
                  <p>Resolved: {formatDateTime(challenge.resolvedAt)}</p>
                  <p>Respect Applied: {challenge.respectPointsAwarded}</p>
                  <p>Karma Applied: {challenge.karmaPointsAwarded}</p>
                  <p>My Role: {challenge.myRole}</p>
                </div>
              </article>
            ))
          )}
        </div>
      </section>
    </section>
  );
}
