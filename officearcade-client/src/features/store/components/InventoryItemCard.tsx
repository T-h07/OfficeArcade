import type { InventoryItem } from "../types/store.types";
import { RarityBadge } from "./RarityBadge";
import {
  formatCategoryLabel,
  formatTimestamp,
  getRarityClassName,
  inventoryItemState
} from "./storePresentation";

type InventoryItemCardProps = {
  item: InventoryItem;
  disabled?: boolean;
  onEquip: (itemId: string) => void;
  onUnequip: (itemId: string) => void;
};

export function InventoryItemCard({ item, disabled = false, onEquip, onUnequip }: InventoryItemCardProps) {
  const state = inventoryItemState(item);
  const cardClass = `oa-reward-item-card ${getRarityClassName(item.rarity)} oa-reward-item-owned ${
    item.equipped ? "oa-reward-item-equipped" : ""
  }`;

  return (
    <article className={cardClass}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[10px] uppercase tracking-[0.2em] text-oa-muted">Owned Cosmetic</p>
          <h3 className="mt-1 text-lg font-semibold text-oa-text">{item.displayName}</h3>
        </div>
        <RarityBadge rarity={item.rarity} />
      </div>

      <p className="mt-2 text-sm text-oa-muted">{item.description}</p>

      <div className="mt-3 flex flex-wrap gap-2">
        <span className="oa-chip">{formatCategoryLabel(item.category)}</span>
        <span className="oa-chip oa-chip-success">Owned</span>
        {state === "EQUIPPED" ? <span className="oa-chip oa-chip-route">Equipped</span> : <span className="oa-chip">Ready</span>}
      </div>

      <div className="mt-3 grid gap-2 rounded-lg border border-oa-border/40 bg-oa-surface-soft/45 px-3 py-2 text-xs text-oa-muted">
        <p>
          Acquired: <span className="font-semibold text-oa-text">{formatTimestamp(item.acquiredAt)}</span>
        </p>
        <p>
          Equipped At: <span className="font-semibold text-oa-text">{formatTimestamp(item.equippedAt)}</span>
        </p>
      </div>

      <button
        type="button"
        onClick={() => {
          if (item.equipped) {
            onUnequip(item.cosmeticItemId);
            return;
          }
          onEquip(item.cosmeticItemId);
        }}
        className={`oa-btn mt-4 w-full px-3 py-2 ${item.equipped ? "oa-btn-secondary" : "oa-btn-primary"}`}
        disabled={disabled}
      >
        {item.equipped ? "Unequip" : "Equip"}
      </button>
    </article>
  );
}
