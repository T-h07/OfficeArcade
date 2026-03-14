type StatusPillProps = {
  label: string;
  tone: "neutral" | "success" | "danger";
};

const TONE_CLASSNAME: Record<StatusPillProps["tone"], string> = {
  neutral: "bg-slate-700/70 text-oa-text",
  success: "bg-emerald-500/20 text-emerald-300 ring-1 ring-emerald-400/30",
  danger: "bg-rose-500/20 text-rose-300 ring-1 ring-rose-400/30"
};

export function StatusPill({ label, tone }: StatusPillProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide ${TONE_CLASSNAME[tone]}`}
    >
      {label}
    </span>
  );
}
