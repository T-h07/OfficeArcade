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
    return "border-fuchsia-300/45 bg-fuchsia-300/15 text-fuchsia-100";
  }
  if (rarity === "RARE") {
    return "border-sky-300/45 bg-sky-300/15 text-sky-100";
  }
  return "border-oa-border bg-black/20 text-oa-muted";
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
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Personalization</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-semibold text-oa-text">My Inventory</h1>
          <span className="rounded-full border border-oa-accent/45 bg-oa-accent/15 px-3 py-1 text-sm font-semibold text-oa-text">
            Respect: {summary?.respectBalance ?? 0}
          </span>
        </div>
        <p className="mt-2 text-sm text-oa-muted">
          Manage owned cosmetics and equipped loadout. One equipped item per category.
        </p>
      </header>

      {errorMessage ? (
        <div className="rounded-xl border border-oa-danger/45 bg-oa-danger/10 px-4 py-3 text-sm text-oa-danger">
          {errorMessage}
        </div>
      ) : null}

      {actionMessage ? (
        <div className="flex items-center justify-between gap-3 rounded-xl border border-oa-accent/45 bg-oa-accent/10 px-4 py-3">
          <p className="text-sm text-oa-text">{actionMessage}</p>
          <button
            type="button"
            onClick={clearActionMessage}
            className="rounded-md border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-muted transition-colors hover:border-oa-accent/45 hover:text-oa-text"
          >
            Dismiss
          </button>
        </div>
      ) : null}

      <section className="grid gap-3 sm:grid-cols-3">
        <article className="rounded-xl border border-oa-border bg-oa-surface/75 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned Items</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{inventory?.ownedCount ?? 0}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface/75 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Items</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{inventory?.equippedCount ?? 0}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface/75 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Summary</p>
          <p className="mt-2 text-sm text-oa-muted">
            {(summary?.equippedItems ?? []).map((item) => item.category).join(", ") || "None"}
          </p>
        </article>
      </section>

      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
        <div className="grid gap-3 md:grid-cols-[1fr_auto]">
          <label className="text-xs uppercase tracking-[0.12em] text-oa-muted">
            Category
            <select
              value={categoryFilter}
              onChange={(event) => setCategoryFilter(event.target.value as CosmeticCategory | "ALL")}
              className="mt-1 w-full rounded-md border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text outline-none focus:border-oa-accent/55"
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
              className="rounded-md border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/50 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isLoading || isMutating}
            >
              {isLoading ? "Refreshing..." : "Refresh"}
            </button>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {filteredItems.length === 0 ? (
          <div className="rounded-xl border border-oa-border bg-oa-surface/75 p-5 text-sm text-oa-muted">
            No owned cosmetics in this category.
          </div>
        ) : (
          filteredItems.map((item) => (
            <article
              key={item.cosmeticItemId}
              className="rounded-xl border border-oa-border bg-oa-surface-soft/65 p-4 transition-colors hover:border-oa-accent/35"
            >
              <div className="flex items-start justify-between gap-2">
                <h3 className="text-base font-semibold text-oa-text">{item.displayName}</h3>
                <span className={`rounded-full border px-2 py-0.5 text-xs ${rarityBadgeClass(item.rarity)}`}>
                  {item.rarity}
                </span>
              </div>

              <p className="mt-2 text-sm text-oa-muted">{item.description}</p>

              <div className="mt-3 flex flex-wrap gap-2">
                <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
                  {item.category}
                </span>
                <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-muted">
                  Acquired: {formatDateTime(item.acquiredAt)}
                </span>
                {item.equipped ? (
                  <span className="rounded-full border border-sky-300/45 bg-sky-300/15 px-2.5 py-1 text-xs text-sky-100">
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
                className="mt-4 w-full rounded-md border border-oa-accent/50 bg-oa-accent/20 px-3 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/30 disabled:cursor-not-allowed disabled:opacity-60"
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
