import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import { PageHero } from "../../layout/PageHero";
import { RewardCategoryTabs } from "../components/RewardCategoryTabs";
import { RespectBalanceCard } from "../components/RespectBalanceCard";
import { StoreItemCard } from "../components/StoreItemCard";
import {
  formatCategoryLabel,
  formatRarityLabel
} from "../components/storePresentation";
import { useStore } from "../hooks/useStore";
import type { CosmeticCategory, CosmeticRarity } from "../types/store.types";

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
  const respectBalance = summary?.respectBalance ?? 0;
  const affordableCount = useMemo(
    () => items.filter((item) => !item.owned && item.priceRespect <= respectBalance).length,
    [items, respectBalance]
  );
  const lockedCount = useMemo(
    () => items.filter((item) => !item.owned && item.priceRespect > respectBalance).length,
    [items, respectBalance]
  );
  const categoryCounts = useMemo(() => {
    const next: Record<CosmeticCategory, number> = {
      HAT: 0,
      GLASSES: 0,
      OUTFIT: 0,
      PROFILE_FRAME: 0,
      BADGE: 0,
      ACCESSORY: 0
    };
    items.forEach((item) => {
      next[item.category] += 1;
    });
    return next;
  }, [items]);
  const rarityCounts = useMemo(() => {
    const next: Record<CosmeticRarity, number> = {
      COMMON: 0,
      RARE: 0,
      EPIC: 0
    };
    items.forEach((item) => {
      next[item.rarity] += 1;
    });
    return next;
  }, [items]);
  const categoryTabOptions = CATEGORY_OPTIONS.map((option) => ({
    value: option,
    label: option === "ALL" ? "All Categories" : formatCategoryLabel(option),
    count: option === "ALL" ? items.length : categoryCounts[option]
  }));
  const rarityTabOptions = RARITY_OPTIONS.map((option) => ({
    value: option,
    label: option === "ALL" ? "All Rarities" : formatRarityLabel(option),
    count: option === "ALL" ? items.length : rarityCounts[option]
  }));

  if (!user) {
    return null;
  }

  return (
    <section className="oa-page">
      <PageHero
        kicker="Reward Vault"
        title="Cosmetic Store"
        subtitle="Spend Respect on profile cosmetics and expand your personal style loadout."
        rightSlot={(
          <RespectBalanceCard
            balance={respectBalance}
            ownedCount={summary?.ownedCount}
            equippedCount={summary?.equippedCount}
            compact
          />
        )}
        footerSlot={(
          <>
            <span className="oa-chip oa-chip-success">Affordable: {affordableCount}</span>
            <span className="oa-chip oa-chip-warning">Locked: {lockedCount}</span>
            <Link to="/app/inventory" className="oa-btn oa-btn-secondary px-3 py-1.5 text-xs">
              Open Inventory
            </Link>
          </>
        )}
      />

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
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Visible Cosmetics</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{items.length}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned In View</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{ownedCount}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Can Unlock Now</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{affordableCount}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Now</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{summary?.equippedCount ?? 0}</p>
        </article>
      </section>

      <section className="oa-panel-soft space-y-4">
        <RewardCategoryTabs
          label="Category"
          options={categoryTabOptions}
          selected={categoryFilter}
          onSelect={setCategoryFilter}
          disabled={isLoading || isMutating}
        />
        <RewardCategoryTabs
          label="Rarity"
          options={rarityTabOptions}
          selected={rarityFilter}
          onSelect={setRarityFilter}
          disabled={isLoading || isMutating}
        />
        <div className="oa-reward-summary-strip">
          <p className="text-xs text-oa-muted">Filter unlocks by category and rarity to prioritize your next cosmetic purchase.</p>
          <button
            type="button"
            onClick={() => {
              void refresh();
            }}
            className="oa-btn oa-btn-secondary px-3 py-2"
            disabled={isLoading || isMutating}
          >
            {isLoading ? "Refreshing..." : "Refresh Catalog"}
          </button>
        </div>
      </section>

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {items.length === 0 ? (
          <div className="oa-empty-state">
            No cosmetics match this filter set yet. Try a broader category or rarity.
          </div>
        ) : (
          items.map((item) => (
            <StoreItemCard
              key={item.id}
              item={item}
              respectBalance={respectBalance}
              disabled={isMutating || isLoading}
              onPurchase={(itemId) => {
                void purchase(itemId);
              }}
              onEquip={(itemId) => {
                void equip(itemId);
              }}
              onUnequip={(itemId) => {
                void unequip(itemId);
              }}
            />
          ))
        )}
      </section>
    </section>
  );
}
