import { appConfig } from "../../../lib/config";
import type {
  CosmeticCategory,
  CosmeticRarity,
  InventoryResponse,
  StoreCatalogResponse,
  StorePurchaseResponse,
  StoreSummaryResponse
} from "../types/store.types";

export class StoreApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
    this.name = "StoreApiError";
  }
}

function resolveUrl(path: string) {
  return `${appConfig.apiBaseUrl}${path}`;
}

function authHeaders(token: string): HeadersInit {
  return {
    Accept: "application/json",
    Authorization: `Bearer ${token}`
  };
}

async function parseError(response: Response, fallback: string): Promise<never> {
  let message = fallback;
  try {
    const payload = (await response.json()) as Record<string, unknown>;
    if (typeof payload.detail === "string" && payload.detail.trim().length > 0) {
      message = payload.detail;
    } else if (typeof payload.message === "string" && payload.message.trim().length > 0) {
      message = payload.message;
    } else if (typeof payload.error === "string" && payload.error.trim().length > 0) {
      message = payload.error;
    }
  } catch {
    // Use fallback if response body is not parseable.
  }
  throw new StoreApiError(response.status, message);
}

function withParams(path: string, params: Record<string, string | undefined>) {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value && value.trim().length > 0) {
      query.set(key, value);
    }
  }
  const encoded = query.toString();
  if (encoded.length === 0) {
    return path;
  }
  return `${path}?${encoded}`;
}

export async function getStoreSummary(token: string): Promise<StoreSummaryResponse> {
  const response = await fetch(resolveUrl("/api/store/me/summary"), {
    method: "GET",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to load store summary.");
  }
  return (await response.json()) as StoreSummaryResponse;
}

export async function getStoreCatalog(
  token: string,
  category?: CosmeticCategory,
  rarity?: CosmeticRarity
): Promise<StoreCatalogResponse> {
  const response = await fetch(
    resolveUrl(withParams("/api/store/catalog", { category, rarity })),
    {
      method: "GET",
      headers: authHeaders(token)
    }
  );
  if (!response.ok) {
    await parseError(response, "Unable to load store catalog.");
  }
  return (await response.json()) as StoreCatalogResponse;
}

export async function purchaseStoreItem(token: string, itemId: string): Promise<StorePurchaseResponse> {
  const response = await fetch(resolveUrl(`/api/store/purchase/${itemId}`), {
    method: "POST",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to purchase selected cosmetic item.");
  }
  return (await response.json()) as StorePurchaseResponse;
}

export async function getMyInventory(token: string): Promise<InventoryResponse> {
  const response = await fetch(resolveUrl("/api/inventory/me"), {
    method: "GET",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to load inventory.");
  }
  return (await response.json()) as InventoryResponse;
}

export async function equipInventoryItem(token: string, itemId: string): Promise<InventoryResponse> {
  const response = await fetch(resolveUrl(`/api/inventory/equip/${itemId}`), {
    method: "POST",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to equip selected cosmetic item.");
  }
  return (await response.json()) as InventoryResponse;
}

export async function unequipInventoryItem(token: string, itemId: string): Promise<InventoryResponse> {
  const response = await fetch(resolveUrl(`/api/inventory/unequip/${itemId}`), {
    method: "POST",
    headers: authHeaders(token)
  });
  if (!response.ok) {
    await parseError(response, "Unable to unequip selected cosmetic item.");
  }
  return (await response.json()) as InventoryResponse;
}
