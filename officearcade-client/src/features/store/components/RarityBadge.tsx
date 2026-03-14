import type { CosmeticRarity } from "../types/store.types";
import { formatRarityLabel, getRarityClassName } from "./storePresentation";

type RarityBadgeProps = {
  rarity: CosmeticRarity;
};

export function RarityBadge({ rarity }: RarityBadgeProps) {
  return <span className={`oa-chip oa-rarity-chip ${getRarityClassName(rarity)}`}>{formatRarityLabel(rarity)}</span>;
}
