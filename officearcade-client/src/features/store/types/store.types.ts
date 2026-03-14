export type CosmeticCategory = "HAT" | "GLASSES" | "OUTFIT" | "PROFILE_FRAME" | "BADGE" | "ACCESSORY";
export type CosmeticRarity = "COMMON" | "RARE" | "EPIC";

export type StoreCatalogItem = {
  id: string;
  code: string;
  displayName: string;
  description: string;
  category: CosmeticCategory;
  rarity: CosmeticRarity;
  priceRespect: number;
  previewAssetKey: string;
  enabled: boolean;
  owned: boolean;
  equipped: boolean;
};

export type StoreCatalogResponse = {
  respectBalance: number;
  totalItems: number;
  items: StoreCatalogItem[];
};

export type EquippedCosmeticSummary = {
  cosmeticItemId: string;
  code: string;
  displayName: string;
  category: CosmeticCategory;
  rarity: CosmeticRarity;
  previewAssetKey: string;
  equippedAt: string;
};

export type StoreSummaryResponse = {
  respectBalance: number;
  ownedCount: number;
  equippedCount: number;
  equippedItems: EquippedCosmeticSummary[];
};

export type StorePurchaseResponse = {
  status: string;
  message: string;
  summary: StoreSummaryResponse;
  item: StoreCatalogItem;
};

export type InventoryItem = {
  cosmeticItemId: string;
  code: string;
  displayName: string;
  description: string;
  category: CosmeticCategory;
  rarity: CosmeticRarity;
  priceRespect: number;
  previewAssetKey: string;
  enabled: boolean;
  equipped: boolean;
  acquiredAt: string;
  equippedAt: string | null;
};

export type InventoryResponse = {
  respectBalance: number;
  ownedCount: number;
  equippedCount: number;
  items: InventoryItem[];
};
