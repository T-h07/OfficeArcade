import type {
  CosmeticCategory,
  CosmeticRarity,
  InventoryItem,
  StoreCatalogItem
} from "../types/store.types";

export type StoreItemVisualState = "CAN_BUY" | "LOCKED" | "OWNED" | "EQUIPPED";

const CATEGORY_LABELS: Record<CosmeticCategory, string> = {
  HAT: "Hat",
  GLASSES: "Glasses",
  OUTFIT: "Outfit",
  PROFILE_FRAME: "Profile Frame",
  BADGE: "Badge",
  ACCESSORY: "Accessory"
};

const RARITY_LABELS: Record<CosmeticRarity, string> = {
  COMMON: "Common",
  RARE: "Rare",
  EPIC: "Epic"
};

export function formatCategoryLabel(category: CosmeticCategory | "ALL") {
  if (category === "ALL") {
    return "All";
  }
  return CATEGORY_LABELS[category];
}

export function formatRarityLabel(rarity: CosmeticRarity | "ALL") {
  if (rarity === "ALL") {
    return "All";
  }
  return RARITY_LABELS[rarity];
}

export function formatRespect(value: number) {
  return value.toLocaleString();
}

export function formatTimestamp(value: string | null) {
  if (!value) {
    return "-";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

export function getRarityClassName(rarity: CosmeticRarity) {
  if (rarity === "EPIC") {
    return "oa-rarity-epic";
  }
  if (rarity === "RARE") {
    return "oa-rarity-rare";
  }
  return "oa-rarity-common";
}

export function resolveStoreItemVisualState(item: StoreCatalogItem, respectBalance: number): StoreItemVisualState {
  if (item.equipped) {
    return "EQUIPPED";
  }
  if (item.owned) {
    return "OWNED";
  }
  return item.priceRespect <= respectBalance ? "CAN_BUY" : "LOCKED";
}

export function inventoryItemState(item: InventoryItem) {
  return item.equipped ? "EQUIPPED" : "OWNED";
}
