import { formatRespect } from "./storePresentation";

type RespectBalanceCardProps = {
  balance: number;
  ownedCount?: number;
  equippedCount?: number;
  compact?: boolean;
};

export function RespectBalanceCard({ balance, ownedCount, equippedCount, compact = false }: RespectBalanceCardProps) {
  return (
    <article className={`oa-respect-balance ${compact ? "oa-respect-balance-compact" : ""}`}>
      <p className="oa-respect-balance-label">Respect Balance</p>
      <p className="oa-respect-balance-value">{formatRespect(balance)}</p>
      {ownedCount !== undefined || equippedCount !== undefined ? (
        <div className="mt-2 flex flex-wrap gap-2">
          {ownedCount !== undefined ? <span className="oa-chip">Owned {ownedCount}</span> : null}
          {equippedCount !== undefined ? <span className="oa-chip oa-chip-route">Equipped {equippedCount}</span> : null}
        </div>
      ) : null}
    </article>
  );
}
