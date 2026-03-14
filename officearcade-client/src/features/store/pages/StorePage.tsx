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
    return "oa-chip-route";
  }
  if (rarity === "RARE") {
    return "oa-chip-info";
  }
  return "";
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
    <section className="oa-page">
      <header className="oa-hero">
        <p className="oa-hero-kicker">Respect Economy</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <h1 className="oa-hero-title mt-0 text-2xl">Store</h1>
          <span className="oa-chip oa-chip-route text-sm font-semibold">
            Respect: {summary?.respectBalance ?? 0}
          </span>
        </div>
        <p className="oa-hero-subtitle">
          Buy cosmetics and shape your profile loadout.
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

      <section className="grid gap-3 sm:grid-cols-4">
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Catalog Items</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{catalog?.totalItems ?? 0}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned in View</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{ownedCount}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Total Owned</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{summary?.ownedCount ?? 0}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Currently Equipped</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{summary?.equippedCount ?? 0}</p>
        </article>
      </section>

      <section className="oa-panel-soft">
        <div className="grid gap-3 md:grid-cols-[1fr_1fr_auto]">
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

          <label className="text-xs uppercase tracking-[0.12em] text-oa-muted">
            Rarity
            <select
              value={rarityFilter}
              onChange={(event) => setRarityFilter(event.target.value as CosmeticRarity | "ALL")}
              className="oa-select mt-1"
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
              className="oa-btn oa-btn-secondary px-3 py-2"
              disabled={isLoading || isMutating}
            >
              {isLoading ? "Refreshing..." : "Refresh"}
            </button>
          </div>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {items.length === 0 ? (
          <div className="oa-empty-state">
            No cosmetics match the current filters.
          </div>
        ) : (
          items.map((item) => (
            <article
              key={item.id}
              className="oa-action-card"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="text-base font-semibold text-oa-text">{item.displayName}</h3>
                  <p className="mt-1 text-xs text-oa-muted">{item.previewAssetKey}</p>
                </div>
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
                  {item.priceRespect} Respect
                </span>
                {item.owned ? (
                  <span className="oa-chip oa-chip-success">
                    Owned
                  </span>
                ) : null}
                {item.equipped ? (
                  <span className="oa-chip oa-chip-info">
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
                className="oa-btn oa-btn-primary mt-4 w-full px-3 py-2"
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
