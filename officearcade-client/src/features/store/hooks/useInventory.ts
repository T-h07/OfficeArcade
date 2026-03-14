import { useCallback, useEffect, useState } from "react";
import {
  equipInventoryItem,
  getMyInventory,
  getStoreSummary,
  StoreApiError,
  unequipInventoryItem
} from "../api/storeApi";
import type { CosmeticCategory, InventoryResponse, StoreSummaryResponse } from "../types/store.types";

type UseInventoryResult = {
  summary: StoreSummaryResponse | null;
  inventory: InventoryResponse | null;
  categoryFilter: CosmeticCategory | "ALL";
  isLoading: boolean;
  isMutating: boolean;
  errorMessage: string | null;
  actionMessage: string | null;
  setCategoryFilter: (value: CosmeticCategory | "ALL") => void;
  refresh: () => Promise<void>;
  equip: (itemId: string) => Promise<boolean>;
  unequip: (itemId: string) => Promise<boolean>;
  clearActionMessage: () => void;
};

function resolveErrorMessage(error: unknown, fallbackMessage: string) {
  if (error instanceof Error && error.message.trim().length > 0) {
    return error.message;
  }
  return fallbackMessage;
}

export function useInventory(accessToken: string | null, onUnauthorized: () => void): UseInventoryResult {
  const [summary, setSummary] = useState<StoreSummaryResponse | null>(null);
  const [inventory, setInventory] = useState<InventoryResponse | null>(null);
  const [categoryFilter, setCategoryFilter] = useState<CosmeticCategory | "ALL">("ALL");
  const [isLoading, setIsLoading] = useState(true);
  const [isMutating, setIsMutating] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    if (!accessToken) {
      setSummary(null);
      setInventory(null);
      setErrorMessage(null);
      setIsLoading(false);
      return;
    }

    setIsLoading(true);
    setErrorMessage(null);

    try {
      const [nextSummary, nextInventory] = await Promise.all([getStoreSummary(accessToken), getMyInventory(accessToken)]);
      setSummary(nextSummary);
      setInventory(nextInventory);
    } catch (error) {
      if (error instanceof StoreApiError && error.status === 401) {
        onUnauthorized();
        return;
      }
      setErrorMessage(resolveErrorMessage(error, "Unable to load inventory data."));
    } finally {
      setIsLoading(false);
    }
  }, [accessToken, onUnauthorized]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function runMutation(
    action: () => Promise<unknown>,
    successMessage: string,
    fallbackErrorMessage: string
  ): Promise<boolean> {
    if (!accessToken) {
      return false;
    }
    setIsMutating(true);
    setErrorMessage(null);
    setActionMessage(null);

    try {
      await action();
      setActionMessage(successMessage);
      await refresh();
      return true;
    } catch (error) {
      if (error instanceof StoreApiError && error.status === 401) {
        onUnauthorized();
        return false;
      }
      setErrorMessage(resolveErrorMessage(error, fallbackErrorMessage));
      return false;
    } finally {
      setIsMutating(false);
    }
  }

  return {
    summary,
    inventory,
    categoryFilter,
    isLoading,
    isMutating,
    errorMessage,
    actionMessage,
    setCategoryFilter,
    refresh,
    equip: (itemId) =>
      runMutation(
        () => equipInventoryItem(accessToken!, itemId),
        "Item equipped.",
        "Unable to equip selected item."
      ),
    unequip: (itemId) =>
      runMutation(
        () => unequipInventoryItem(accessToken!, itemId),
        "Item unequipped.",
        "Unable to unequip selected item."
      ),
    clearActionMessage: () => setActionMessage(null)
  };
}
