import { useMemo } from "react";
import { useAuth } from "../../auth/AuthContext";
import { useStore } from "../hooks/useStore";
import type { CosmeticCategory, CosmeticRarity, StoreCatalogItem } from "../types/store.types";

const CATEGORY_OPTIONS: Array<CosmeticCategory | "ALL"> = [
  "ALL",
  "HAT",
  "GLASSES",
  "OUTFIT",
  "PROFILE_FRAME",
  "BADGE",
  "ACCESSORY"
];
const RARITY_OPTIONS: Array<CosmeticRarity | "ALL"> = ["ALL", "COMMON", "RARE", "EPIC"];

function rarityBadgeClass(rarity: CosmeticRarity) {
  if (rarity === "EPIC") {
    return "border-fuchsia-300/45 bg-fuchsia-300/15 text-fuchsia-100";
  }
  if (rarity === "RARE") {
    return "border-sky-300/45 bg-sky-300/15 text-sky-100";
  }
  return "border-oa-border bg-black/20 text-oa-muted";
}

function renderActionLabel(item: StoreCatalogItem) {
  if (!item.owned) {
    return `Buy (${item.priceRespect} Respect)`;
  }
  if (item.equipped) {
    return "Unequip";
  }
  return "Equip";
}

export function StorePage() {
  const { accessToken, logout, user } = useAuth();
  const {
    summary,
    catalog,
    categoryFilter,
    rarityFilter,
    isLoading,
    isMutating,
    errorMessage,
    actionMessage,
    setCategoryFilter,
    setRarityFilter,
    refresh,
    purchase,
    equip,
    unequip,
    clearActionMessage
  } = useStore(accessToken, logout);

  const items = catalog?.items ?? [];
  const ownedCount = useMemo(() => items.filter((item) => item.owned).length, [items]);

  if (!user) {
    return null;
  }

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Respect Economy</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-semibold text-oa-text">Store</h1>
          <span className="rounded-full border border-oa-accent/45 bg-oa-accent/15 px-3 py-1 text-sm font-semibold text-oa-text">
            Respect: {summary?.respectBalance ?? 0}
          </span>
        </div>
        <p className="mt-2 text-sm text-oa-muted">
          Spend Respect on workplace-safe cosmetics. Ownership and equip state persist per account.
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

      <section className="grid gap-3 sm:grid-cols-4">
        <article className="rounded-xl border border-oa-border bg-oa-surface/75 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Catalog Items</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{catalog?.totalItems ?? 0}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface/75 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned in View</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{ownedCount}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface/75 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Total Owned</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{summary?.ownedCount ?? 0}</p>
        </article>
        <article className="rounded-xl border border-oa-border bg-oa-surface/75 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Currently Equipped</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{summary?.equippedCount ?? 0}</p>
        </article>
      </section>

      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-4">
        <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
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

          <label className="text-xs uppercase tracking-[0.12em] text-oa-muted">
            Rarity
            <select
              value={rarityFilter}
              onChange={(event) => setRarityFilter(event.target.value as CosmeticRarity | "ALL")}
              className="mt-1 w-full rounded-md border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text outline-none focus:border-oa-accent/55"
              disabled={isLoading || isMutating}
            >
              {RARITY_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option === "ALL" ? "All Rarities" : option}
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
        {items.length === 0 ? (
          <div className="rounded-xl border border-oa-border bg-oa-surface/75 p-5 text-sm text-oa-muted">
            No cosmetics match the current filters.
          </div>
        ) : (
          items.map((item) => (
            <article
              key={item.id}
              className="rounded-xl border border-oa-border bg-oa-surface-soft/65 p-4 transition-colors hover:border-oa-accent/35"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="text-base font-semibold text-oa-text">{item.displayName}</h3>
                  <p className="mt-1 text-xs text-oa-muted">{item.previewAssetKey}</p>
                </div>
                <span className={`rounded-full border px-2.5 py-1 text-xs ${rarityBadgeClass(item.rarity)}`}>
                  {item.rarity}
                </span>
              </div>

              <p className="mt-2 text-sm text-oa-muted">{item.description}</p>

              <div className="mt-3 flex flex-wrap gap-2">
                <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
                  {item.category}
                </span>
                <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
                  {item.priceRespect} Respect
                </span>
                {item.owned ? (
                  <span className="rounded-full border border-oa-accent/45 bg-oa-accent/15 px-2.5 py-1 text-xs text-oa-text">
                    Owned
                  </span>
                ) : null}
                {item.equipped ? (
                  <span className="rounded-full border border-sky-300/45 bg-sky-300/15 px-2.5 py-1 text-xs text-sky-100">
                    Equipped
                  </span>
                ) : null}
              </div>

              <button
                type="button"
                onClick={() => {
                  if (!item.owned) {
                    void purchase(item.id);
                    return;
                  }
                  if (item.equipped) {
                    void unequip(item.id);
                    return;
                  }
                  void equip(item.id);
                }}
                className="mt-4 w-full rounded-md border border-oa-accent/50 bg-oa-accent/20 px-3 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/30 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={isMutating || isLoading}
              >
                {renderActionLabel(item)}
              </button>
            </article>
          ))
        )}
      </section>
    </section>
  );
}
