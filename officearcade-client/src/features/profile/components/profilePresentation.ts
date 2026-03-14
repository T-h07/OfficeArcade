import type { CosmeticCategory } from "../../store/types/store.types";

const CATEGORY_LABELS: Record<CosmeticCategory, string> = {
  HAT: "Hat",
  GLASSES: "Glasses",
  OUTFIT: "Outfit",
  PROFILE_FRAME: "Profile Frame",
  BADGE: "Badge",
  ACCESSORY: "Accessory"
};

export function formatProfileCategoryLabel(category: CosmeticCategory) {
  return CATEGORY_LABELS[category];
}

export function formatProfileDateTime(value: string | null) {
  if (!value) {
    return "-";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

export function initialLettersOf(name: string) {
  const tokens = name
    .split(" ")
    .map((token) => token.trim())
    .filter((token) => token.length > 0);
  if (tokens.length === 0) {
    return "OA";
  }
  if (tokens.length === 1) {
    return tokens[0].slice(0, 2).toUpperCase();
  }
  return `${tokens[0][0]}${tokens[1][0]}`.toUpperCase();
}
