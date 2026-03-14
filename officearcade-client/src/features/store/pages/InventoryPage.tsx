import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import { PageHero } from "../../layout/PageHero";
import { InventoryItemCard } from "../components/InventoryItemCard";
import { RewardCategoryTabs } from "../components/RewardCategoryTabs";
import { RespectBalanceCard } from "../components/RespectBalanceCard";
import { formatCategoryLabel } from "../components/storePresentation";
import { useInventory } from "../hooks/useInventory";
import type { CosmeticCategory } from "../types/store.types";

const CATEGORY_OPTIONS: Array<CosmeticCategory | "ALL"> = [
  "ALL",
  "HAT",
  "GLASSES",
  "OUTFIT",
  "PROFILE_FRAME",
  "BADGE",
  "ACCESSORY"
];

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
  const categoryCounts = useMemo(() => {
    const all = inventory?.items ?? [];
    const next: Record<CosmeticCategory, number> = {
      HAT: 0,
      GLASSES: 0,
      OUTFIT: 0,
      PROFILE_FRAME: 0,
      BADGE: 0,
      ACCESSORY: 0
    };
    all.forEach((item) => {
      next[item.category] += 1;
    });
    return next;
  }, [inventory?.items]);
  const categoryTabOptions = CATEGORY_OPTIONS.map((option) => ({
    value: option,
    label: option === "ALL" ? "All Categories" : formatCategoryLabel(option),
    count: option === "ALL" ? inventory?.ownedCount ?? 0 : categoryCounts[option]
  }));
  const equippedItemLabels = summary?.equippedItems ?? [];
  const uniqueOwnedCategories = useMemo(() => {
    const all = inventory?.items ?? [];
    return new Set(all.map((item) => item.category)).size;
  }, [inventory?.items]);

  if (!user) {
    return null;
  }

  return (
    <section className="oa-page">
      <PageHero
        kicker="Collection Vault"
        title="My Inventory"
        subtitle="Review owned cosmetics, manage equipped slots, and keep your profile loadout tuned."
        rightSlot={(
          <RespectBalanceCard
            balance={summary?.respectBalance ?? 0}
            ownedCount={inventory?.ownedCount}
            equippedCount={inventory?.equippedCount}
            compact
          />
        )}
        footerSlot={(
          <>
            <span className="oa-chip">Categories Built: {uniqueOwnedCategories}</span>
            <span className="oa-chip oa-chip-route">Equipped Slots: {summary?.equippedCount ?? 0}</span>
            <Link to="/app/profile" className="oa-btn oa-btn-secondary px-3 py-1.5 text-xs">
              Open Profile Loadout
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
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned Items</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{inventory?.ownedCount ?? 0}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Items</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{inventory?.equippedCount ?? 0}</p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Categories</p>
          <p className="mt-2 text-sm text-oa-muted">
            {(summary?.equippedItems ?? []).map((item) => item.category).join(", ") || "None"}
          </p>
        </article>
        <article className="oa-kpi-card">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Collection Coverage</p>
          <p className="mt-2 text-2xl font-semibold text-oa-text">{uniqueOwnedCategories}</p>
        </article>
      </section>

      <section className="oa-panel-soft space-y-4">
        <RewardCategoryTabs
          label="Collection Category"
          options={categoryTabOptions}
          selected={categoryFilter}
          onSelect={setCategoryFilter}
          disabled={isLoading || isMutating}
        />
        <div className="oa-reward-summary-strip">
          <p className="text-xs text-oa-muted">Switch categories to focus on loadout slots and quickly equip from owned cosmetics.</p>
          <button
            type="button"
            onClick={() => {
              void refresh();
            }}
            className="oa-btn oa-btn-secondary px-3 py-2"
            disabled={isLoading || isMutating}
          >
            {isLoading ? "Refreshing..." : "Refresh Inventory"}
          </button>
        </div>
      </section>

      <section className="oa-reward-summary-strip">
        <div className="space-y-2">
          <p className="text-xs uppercase tracking-[0.16em] text-oa-muted">Equipped Loadout</p>
          <div className="flex flex-wrap gap-2">
            {equippedItemLabels.length === 0 ? (
              <span className="oa-chip">No cosmetics equipped yet.</span>
            ) : (
              equippedItemLabels.map((item) => (
                <span key={item.cosmeticItemId} className="oa-chip oa-chip-route">
                  {formatCategoryLabel(item.category)}: {item.displayName}
                </span>
              ))
            )}
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
            <InventoryItemCard
              key={item.cosmeticItemId}
              item={item}
              disabled={isMutating || isLoading}
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
