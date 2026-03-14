import { FormEvent, useEffect, useState } from "react";
import type { CreateModerationReportRequest, ModerationReportCategory } from "../types/moderation.types";

type ReportContext = {
  sourceRoomId?: string;
  sourceGameSessionId?: string;
  sourceChallengeId?: string;
  defaultCategory?: ModerationReportCategory;
};

type ReportUserModalProps = {
  isOpen: boolean;
  reportedUserId: string;
  reportedDisplayName: string;
  context?: ReportContext;
  isSubmitting: boolean;
  errorMessage: string | null;
  onClose: () => void;
  onSubmit: (request: CreateModerationReportRequest) => Promise<void>;
};

const CATEGORY_OPTIONS: Array<{ value: ModerationReportCategory; label: string }> = [
  { value: "UNSPORTSMANLIKE_BEHAVIOR", label: "Unsportsmanlike Behavior" },
  { value: "CHALLENGE_DISPUTE", label: "Challenge Dispute" },
  { value: "HARASSMENT_OR_INAPPROPRIATE_BEHAVIOR", label: "Harassment / Inappropriate" },
  { value: "ABUSE_OF_SYSTEM", label: "Abuse of System" },
  { value: "OTHER", label: "Other" }
];

export function ReportUserModal({
  isOpen,
  reportedUserId,
  reportedDisplayName,
  context,
  isSubmitting,
  errorMessage,
  onClose,
  onSubmit
}: ReportUserModalProps) {
  const [category, setCategory] = useState<ModerationReportCategory>(context?.defaultCategory ?? "UNSPORTSMANLIKE_BEHAVIOR");
  const [note, setNote] = useState("");

  useEffect(() => {
    if (!isOpen) {
      return;
    }
    setCategory(context?.defaultCategory ?? "UNSPORTSMANLIKE_BEHAVIOR");
    setNote("");
  }, [context?.defaultCategory, isOpen, reportedUserId]);

  if (!isOpen) {
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    await onSubmit({
      reportedUserId,
      category,
      note,
      sourceRoomId: context?.sourceRoomId,
      sourceGameSessionId: context?.sourceGameSessionId,
      sourceChallengeId: context?.sourceChallengeId
    });
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/65 p-4">
      <section className="w-full max-w-lg rounded-2xl border border-oa-border bg-oa-surface/95 p-5 shadow-glow">
        <div className="flex items-start justify-between gap-3">
          <div>
            <p className="text-xs uppercase tracking-[0.14em] text-oa-muted">Safety Report</p>
            <h2 className="mt-1 text-lg font-semibold text-oa-text">Report {reportedDisplayName}</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md border border-oa-border bg-black/30 px-2.5 py-1 text-xs text-oa-muted transition-colors hover:border-oa-accent/45 hover:text-oa-text"
            disabled={isSubmitting}
          >
            Close
          </button>
        </div>

        <form className="mt-4 space-y-3" onSubmit={handleSubmit}>
          <div>
            <label className="mb-2 block text-sm text-oa-muted" htmlFor="report-category">
              Category
            </label>
            <select
              id="report-category"
              value={category}
              onChange={(event) => setCategory(event.target.value as ModerationReportCategory)}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
              disabled={isSubmitting}
            >
              {CATEGORY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="mb-2 block text-sm text-oa-muted" htmlFor="report-note">
              Note (optional, 280 chars)
            </label>
            <textarea
              id="report-note"
              value={note}
              onChange={(event) => setNote(event.target.value.slice(0, 280))}
              rows={4}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
              placeholder="Keep this short and factual."
              disabled={isSubmitting}
            />
            <p className="mt-1 text-xs text-oa-muted">{note.length}/280</p>
          </div>

          {errorMessage ? (
            <p className="rounded-lg border border-oa-danger/45 bg-oa-danger/10 px-3 py-2 text-sm text-oa-danger">
              {errorMessage}
            </p>
          ) : null}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg border border-oa-accent/55 bg-oa-accent/25 px-4 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/35 disabled:cursor-not-allowed disabled:opacity-65"
          >
            {isSubmitting ? "Submitting..." : "Submit Report"}
          </button>
        </form>
      </section>
    </div>
  );
}
