type KpiTileProps = {
  label: string;
  value: string;
  helperText?: string;
};

export function KpiTile({ label, value, helperText }: KpiTileProps) {
  return (
    <article className="rounded-xl border border-oa-border bg-oa-surface-soft/60 p-4">
      <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">{label}</p>
      <p className="mt-2 text-2xl font-semibold text-oa-text">{value}</p>
      {helperText ? <p className="mt-1 text-xs text-oa-muted">{helperText}</p> : null}
    </article>
  );
}
