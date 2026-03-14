import type { AppRole } from "../../auth/auth.types";
import type { CosmeticCategory, CosmeticRarity } from "../../store/types/store.types";

export type ProfileLayerCategory = CosmeticCategory | "BASE_BODY";

export type ProfileOwnedCosmetic = {
  cosmeticItemId: string;
  code: string;
  displayName: string;
  description: string;
  category: CosmeticCategory;
  rarity: CosmeticRarity;
  previewAssetKey: string;
  enabled: boolean;
  equipped: boolean;
  acquiredAt: string;
  equippedAt: string | null;
};

export type ProfileEquippedCosmetic = {
  cosmeticItemId: string;
  code: string;
  displayName: string;
  category: CosmeticCategory;
  rarity: CosmeticRarity;
  previewAssetKey: string;
  layerOrder: number;
  equippedAt: string;
};

export type ProfileAvatarLayer = {
  layerKey: string;
  category: ProfileLayerCategory;
  layerOrder: number;
  displayName: string;
  previewAssetKey: string;
  source: "BASE" | "COSMETIC";
  cosmeticItemId: string | null;
};

export type ProfileMeResponse = {
  userId: string;
  displayName: string;
  email: string;
  role: AppRole;
  accountEnabled: boolean;
  level: number;
  xp: number;
  respectPoints: number;
  karmaPoints: number;
  ownedCosmeticCount: number;
  equippedCosmeticCount: number;
  ownedCosmetics: ProfileOwnedCosmetic[];
  equippedCosmetics: ProfileEquippedCosmetic[];
  avatarLayers: ProfileAvatarLayer[];
  profileUpdatedAt: string;
  generatedAt: string;
};

export const PROFILE_CUSTOMIZATION_CATEGORY_ORDER: CosmeticCategory[] = [
  "OUTFIT",
  "ACCESSORY",
  "GLASSES",
  "HAT",
  "PROFILE_FRAME",
  "BADGE"
];
