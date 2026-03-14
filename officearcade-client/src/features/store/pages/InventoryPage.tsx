import { useMemo } from "react";
import { useAuth } from "../../auth/AuthContext";
import { useInventory } from "../hooks/useInventory";
import type { CosmeticCategory, InventoryItem } from "../types/store.types";

const CATEGORY_OPTIONS: Array<CosmeticCategory | "ALL"> = [
  "ALL",
  "HAT",
  "GLASSES",
  "OUTFIT",
  "PROFILE_FRAME",
  "BADGE",
  "ACCESSORY"
];

function formatDateTime(value: string | null) {
  if (!value) {
    return "-";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleString();
}

function rarityBadgeClass(rarity: InventoryItem["rarity"]) {
  if (rarity === "EPIC") {
    return "oa-chip-route";
  }
  if (rarity === "RARE") {
    return "oa-chip-info";
  }
  return "";
}

export function InventoryPage() {
  const { accessToken, logout, user } = useAuth();
  const {
    summary,
    inventory,
    categoryFilter,
    isLoading,
    isMutating,
    errorMessage,
    actionMessage,
    setCategoryFilter,
    refresh,
    equip,
    unequip,
    clearActionMessage
  } = useInventory(accessToken, logout);

  const filteredItems = useMemo(() => {
    const all = inventory?.items ?? [];
    if (categoryFilter === "ALL") {
      return all;
    }
    return all.filter((item) => item.category === categoryFilter);
  }, [inventory?.items, categoryFilter]);

  if (!user) {
    return null;
  }

  return (
    <section className="oa-page">
      <header className="oa-hero">
        <p className="oa-hero-kicker">Personalization</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <h1 className="oa-hero-title mt-0 text-2xl">My Inventory</h1>
          <span className="oa-chip oa-chip-route text-sm font-semibold">
            Respect: {summary?.respectBalance ?? 0}
          </span>
        </div>
        <p className="oa-hero-subtitle">
          Manage owned cosmetics and active loadout slots.
        </p>
      </header>

      {errorMessage ? (
        <div className="oa-alert oa-alert-danger">
          {errorMessage}
        </div>
      ) : null}

      {actionMessage ? (
        <div className="oa-alert oa-alert-success flex items-center justify-between gap-3">
          <p className="text-sm text-oa-text">{actionMessage}</p>
          <button
            type="button"
            onClick={clearActionMessage}
            className="oa-btn oa-btn-ghost px-2.5 py-1 text-xs"
          >
            Dismiss
          </button>
        </div>
      ) : null}

      <section className="grid gap-3 sm:grid-cols-3">
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned Items</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{inventory?.ownedCount ?? 0}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Items</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{inventory?.equippedCount ?? 0}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Summary</p>
          <p className="mt-2 text-sm text-oa-muted">
            {(summary?.equippedItems ?? []).map((item) => item.category).join(", ") || "None"}
          </p>
        </article>
      </section>

      <section className="oa-panel-soft">
        <div className="grid gap-3 md:grid-cols-[1fr_auto]">
          <label className="text-xs uppercase tracking-[0.12em] text-oa-muted">
            Category
            <select
              value={categoryFilter}
              onChange={(event) => setCategoryFilter(event.target.value as CosmeticCategory | "ALL")}
              className="oa-select mt-1"
              disabled={isLoading || isMutating}
            >
              {CATEGORY_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option === "ALL" ? "All Categories" : option}
                </option>
              ))}
            </select>
          </label>
          <div className="flex items-end">
            <button
              type="button"
              onClick={() => {
                void refresh();
              }}
              className="oa-btn oa-btn-secondary px-3 py-2"
              disabled={isLoading || isMutating}
            >
              {isLoading ? "Refreshing..." : "Refresh"}
            </button>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {filteredItems.length === 0 ? (
          <div className="oa-empty-state">
            No owned cosmetics in this category.
          </div>
        ) : (
          filteredItems.map((item) => (
            <article
              key={item.cosmeticItemId}
              className="oa-action-card"
            >
              <div className="flex items-start justify-between gap-2">
                <h3 className="text-base font-semibold text-oa-text">{item.displayName}</h3>
                <span className={`oa-chip ${rarityBadgeClass(item.rarity)}`}>
                  {item.rarity}
                </span>
              </div>

              <p className="mt-2 text-sm text-oa-muted">{item.description}</p>

              <div className="mt-3 flex flex-wrap gap-2">
                <span className="oa-chip">
                  {item.category}
                </span>
                <span className="oa-chip">
                  Acquired: {formatDateTime(item.acquiredAt)}
                </span>
                {item.equipped ? (
                  <span className="oa-chip oa-chip-info">
                    Equipped
                  </span>
                ) : null}
              </div>

              <button
                type="button"
                onClick={() => {
                  if (item.equipped) {
                    void unequip(item.cosmeticItemId);
                    return;
                  }
                  void equip(item.cosmeticItemId);
                }}
                className="oa-btn oa-btn-primary mt-4 w-full px-3 py-2"
                disabled={isMutating || isLoading}
              >
                {item.equipped ? "Unequip" : "Equip"}
              </button>
            </article>
          ))
        )}
      </section>
    </section>
  );
}
