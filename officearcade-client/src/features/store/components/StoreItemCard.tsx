import type { StoreCatalogItem } from "../types/store.types";
import { RarityBadge } from "./RarityBadge";
import {
  formatCategoryLabel,
  formatRespect,
  getRarityClassName,
  resolveStoreItemVisualState
} from "./storePresentation";

type StoreItemCardProps = {
  item: StoreCatalogItem;
  respectBalance: number;
  disabled?: boolean;
  onPurchase: (itemId: string) => void;
  onEquip: (itemId: string) => void;
  onUnequip: (itemId: string) => void;
};

function resolveActionLabel(item: StoreCatalogItem, respectBalance: number) {
  const state = resolveStoreItemVisualState(item, respectBalance);
  if (state === "EQUIPPED") {
    return "Unequip";
  }
  if (state === "OWNED") {
    return "Equip";
  }
  if (state === "LOCKED") {
    return `Need ${formatRespect(item.priceRespect - respectBalance)} Respect`;
  }
  return `Buy (${formatRespect(item.priceRespect)})`;
}

function resolveActionClassName(item: StoreCatalogItem, respectBalance: number) {
  const state = resolveStoreItemVisualState(item, respectBalance);
  if (state === "EQUIPPED") {
    return "oa-btn oa-btn-secondary mt-4 w-full px-3 py-2";
  }
  if (state === "LOCKED") {
    return "oa-btn oa-btn-ghost mt-4 w-full px-3 py-2";
  }
  return "oa-btn oa-btn-primary mt-4 w-full px-3 py-2";
}

export function StoreItemCard({ item, respectBalance, disabled = false, onPurchase, onEquip, onUnequip }: StoreItemCardProps) {
  const state = resolveStoreItemVisualState(item, respectBalance);
  const canAfford = item.priceRespect <= respectBalance;
  const baseClass = `oa-reward-item-card ${getRarityClassName(item.rarity)} ${item.owned ? "oa-reward-item-owned" : ""} ${
    item.equipped ? "oa-reward-item-equipped" : ""
  } ${state === "LOCKED" ? "oa-reward-item-locked" : ""}`;

  return (
    <article className={baseClass}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] uppercase tracking-[0.2em] text-oa-muted">Cosmetic</p>
          <h3 className="mt-1 text-lg font-semibold text-oa-text">{item.displayName}</h3>
        </div>
        <RarityBadge rarity={item.rarity} />
      </div>

      <p className="mt-2 text-sm text-oa-muted">{item.description}</p>

      <div className="mt-3 flex flex-wrap gap-2">
        <span className="oa-chip">{formatCategoryLabel(item.category)}</span>
        <span className={`oa-chip ${canAfford || item.owned ? "oa-chip-success" : "oa-chip-warning"}`}>
          {item.owned ? "Owned" : `${formatRespect(item.priceRespect)} Respect`}
        </span>
        {item.equipped ? <span className="oa-chip oa-chip-route">Equipped</span> : null}
        {!item.owned && canAfford ? <span className="oa-chip oa-chip-success">Affordable</span> : null}
      </div>

      <div className="mt-3 rounded-lg border border-oa-border/40 bg-oa-surface-soft/45 px-3 py-2 text-xs text-oa-muted">
        Asset Key: <span className="font-semibold text-oa-text">{item.previewAssetKey}</span>
      </div>

      <button
        type="button"
        onClick={() => {
          if (!item.owned) {
            onPurchase(item.id);
            return;
          }
          if (item.equipped) {
            onUnequip(item.id);
            return;
          }
          onEquip(item.id);
        }}
        className={resolveActionClassName(item, respectBalance)}
        disabled={disabled || state === "LOCKED"}
      >
        {resolveActionLabel(item, respectBalance)}
      </button>
    </article>
  );
}
