import type { CosmeticCategory } from "../../store/types/store.types";
import type { ProfileEquippedCosmetic } from "../types/profile.types";
import { formatProfileCategoryLabel } from "./profilePresentation";

type ProfileLoadoutSlotsProps = {
  categories: CosmeticCategory[];
  selectedCategory: CosmeticCategory;
  equippedByCategory: Partial<Record<CosmeticCategory, ProfileEquippedCosmetic>>;
  disabled?: boolean;
  onSelect: (category: CosmeticCategory) => void;
};

export function ProfileLoadoutSlots({
  categories,
  selectedCategory,
  equippedByCategory,
  disabled = false,
  onSelect
}: ProfileLoadoutSlotsProps) {
  return (
    <section className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Loadout Slots</p>
          <p className="mt-1 text-sm text-oa-muted">Choose a category, then equip items to update your character stage.</p>
        </div>
      </div>
      <div className="oa-loadout-slot-grid">
        {categories.map((category) => {
          const equipped = equippedByCategory[category];
          const isSelected = selectedCategory === category;
          return (
            <button
              key={category}
              type="button"
              onClick={() => onSelect(category)}
              className={`oa-loadout-slot-btn ${isSelected ? "oa-loadout-slot-btn-active" : ""}`}
              disabled={disabled}
            >
              <div>
                <p className="text-[10px] uppercase tracking-[0.16em] text-oa-muted">{formatProfileCategoryLabel(category)}</p>
                <p className="mt-1 text-sm font-semibold text-oa-text">{equipped?.displayName ?? "Empty Slot"}</p>
              </div>
              <span className={`oa-chip ${equipped ? "oa-chip-route" : ""}`}>{equipped ? "Equipped" : "None"}</span>
            </button>
          );
        })}
      </div>
    </section>
  );
}
