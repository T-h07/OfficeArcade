import { RarityBadge } from "../../store/components/RarityBadge";
import type { ProfileOwnedCosmetic } from "../types/profile.types";
import { formatProfileCategoryLabel, formatProfileDateTime } from "./profilePresentation";

type ProfileOwnedItemCardProps = {
  item: ProfileOwnedCosmetic;
  disabled?: boolean;
  onEquip: (itemId: string) => void;
  onUnequip: (itemId: string) => void;
};

export function ProfileOwnedItemCard({ item, disabled = false, onEquip, onUnequip }: ProfileOwnedItemCardProps) {
  return (
    <article className={`oa-reward-item-card ${item.equipped ? "oa-reward-item-equipped" : "oa-reward-item-owned"}`}>
      <div className="flex items-start justify-between gap-2">
        <div>
          <p className="text-[10px] uppercase tracking-[0.2em] text-oa-muted">Owned Cosmetic</p>
          <h3 className="mt-1 text-lg font-semibold text-oa-text">{item.displayName}</h3>
        </div>
        <RarityBadge rarity={item.rarity} />
      </div>

      <p className="mt-2 text-sm text-oa-muted">{item.description}</p>

      <div className="mt-3 flex flex-wrap gap-2">
        <span className="oa-chip">{formatProfileCategoryLabel(item.category)}</span>
        <span className="oa-chip oa-chip-success">Owned</span>
        {item.equipped ? <span className="oa-chip oa-chip-route">Equipped</span> : <span className="oa-chip">Ready</span>}
        {!item.enabled ? <span className="oa-chip oa-chip-danger">Disabled</span> : null}
      </div>

      <div className="mt-3 grid gap-2 rounded-lg border border-oa-border/40 bg-oa-surface-soft/45 px-3 py-2 text-xs text-oa-muted">
        <p>
          Acquired: <span className="font-semibold text-oa-text">{formatProfileDateTime(item.acquiredAt)}</span>
        </p>
        <p>
          Equipped At: <span className="font-semibold text-oa-text">{formatProfileDateTime(item.equippedAt)}</span>
        </p>
        <p>
          Asset Key: <span className="font-semibold text-oa-text">{item.previewAssetKey}</span>
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
        disabled={disabled || !item.enabled}
      >
        {item.equipped ? "Unequip" : "Equip"}
      </button>
    </article>
  );
}
