import { useMemo } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../auth/AuthContext";
import { PageHero } from "../../layout/PageHero";
import type { CosmeticCategory } from "../../store/types/store.types";
import { AvatarLoadoutPreview } from "../components/AvatarLoadoutPreview";
import { ProfileLoadoutSlots } from "../components/ProfileLoadoutSlots";
import { ProfileOwnedItemCard } from "../components/ProfileOwnedItemCard";
import { formatProfileCategoryLabel } from "../components/profilePresentation";
import { useProfileCustomization } from "../hooks/useProfileCustomization";
import type { ProfileEquippedCosmetic } from "../types/profile.types";
import { PROFILE_CUSTOMIZATION_CATEGORY_ORDER } from "../types/profile.types";

function LoadingState() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="oa-panel h-24" />
      <div className="grid gap-4 xl:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
        <div className="oa-panel h-[620px]" />
        <div className="oa-panel h-[620px]" />
      </div>
      <div className="oa-panel-soft h-36" />
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        <div className="oa-panel h-72" />
        <div className="oa-panel h-72" />
        <div className="oa-panel h-72" />
      </div>
    </div>
  );
}

export function ProfileCustomizationPage() {
  const { accessToken, logout, user } = useAuth();
  const {
    profile,
    selectedCategory,
    itemsInSelectedCategory,
    isLoading,
    isMutating,
    errorMessage,
    actionMessage,
    setSelectedCategory,
    refresh,
    equip,
    unequip,
    clearActionMessage
  } = useProfileCustomization(accessToken, logout);

  const equippedByCategory = useMemo(() => {
    const map: Partial<Record<CosmeticCategory, ProfileEquippedCosmetic>> = {};
    (profile?.equippedCosmetics ?? []).forEach((item) => {
      map[item.category] = item;
    });
    return map;
  }, [profile?.equippedCosmetics]);

  const filledSlotCount = useMemo(
    () => PROFILE_CUSTOMIZATION_CATEGORY_ORDER.filter((category) => Boolean(equippedByCategory[category])).length,
    [equippedByCategory]
  );

  if (!user) {
    return null;
  }

  return (
    <section className="oa-page">
      <PageHero
        kicker="Identity Chamber"
        title="Profile & Character Bay"
        subtitle="Shape your arcade persona with equipped cosmetics, tuned slots, and a live character stage preview."
        rightSlot={(
          <div className="flex flex-wrap gap-2">
            <span className="oa-chip">Level {profile?.level ?? "-"}</span>
            <span className="oa-chip oa-chip-route">Respect {profile?.respectPoints ?? 0}</span>
            <span className="oa-chip oa-chip-warning">Karma {profile?.karmaPoints ?? 0}</span>
          </div>
        )}
        footerSlot={(
          <>
            <span className="oa-chip">Slots Filled: {filledSlotCount}/{PROFILE_CUSTOMIZATION_CATEGORY_ORDER.length}</span>
            <span className="oa-chip oa-chip-success">Owned: {profile?.ownedCosmeticCount ?? 0}</span>
            <Link to="/app/inventory" className="oa-btn oa-btn-secondary px-3 py-1.5 text-xs">
              Manage Inventory
            </Link>
            <Link to="/app/store" className="oa-btn oa-btn-ghost px-3 py-1.5 text-xs">
              Open Store
            </Link>
          </>
        )}
      />

      {isLoading ? <LoadingState /> : null}

      {!isLoading && errorMessage ? <div className="oa-alert oa-alert-danger">{errorMessage}</div> : null}

      {actionMessage ? (
        <div className="oa-alert oa-alert-success flex items-center justify-between gap-3">
          <p className="text-sm text-oa-text">{actionMessage}</p>
          <button type="button" onClick={clearActionMessage} className="oa-btn oa-btn-ghost px-2.5 py-1 text-xs">
            Dismiss
          </button>
        </div>
      ) : null}

      {!isLoading && !errorMessage && profile ? (
        <div className="space-y-5">
          <section className="grid gap-4 xl:grid-cols-[minmax(0,420px)_minmax(0,1fr)]">
            <AvatarLoadoutPreview
              displayName={profile.displayName}
              layers={profile.avatarLayers}
              selectedCategory={selectedCategory}
            />

            <article className="oa-panel space-y-4">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Arcade Identity</p>
                  <h2 className="mt-1 text-2xl font-semibold text-oa-text">{profile.displayName}</h2>
                  <p className="mt-1 text-sm text-oa-muted">{profile.email}</p>
                </div>
                <div className="flex flex-wrap items-center gap-2">
                  <span className="oa-chip">{profile.role}</span>
                  <span className={`oa-chip ${profile.department?.active ? "oa-chip-route" : ""}`}>
                    {profile.department ? `${profile.department.displayName} (${profile.department.code})` : "Unassigned"}
                  </span>
                  <span className={`oa-chip ${profile.accountEnabled ? "oa-chip-success" : "oa-chip-danger"}`}>
                    {profile.accountEnabled ? "Active" : "Inactive"}
                  </span>
                </div>
              </div>

              <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                <div className="oa-kpi-card">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Level</p>
                  <p className="mt-2 text-2xl font-semibold text-oa-text">{profile.level}</p>
                </div>
                <div className="oa-kpi-card">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">XP</p>
                  <p className="mt-2 text-2xl font-semibold text-oa-text">{profile.xp}</p>
                </div>
                <div className="oa-kpi-card">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Respect</p>
                  <p className="mt-2 text-2xl font-semibold text-oa-text">{profile.respectPoints}</p>
                </div>
                <div className="oa-kpi-card">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Karma</p>
                  <p className="mt-2 text-2xl font-semibold text-oa-text">{profile.karmaPoints}</p>
                </div>
                <div className="oa-kpi-card">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned Cosmetics</p>
                  <p className="mt-2 text-2xl font-semibold text-oa-text">{profile.ownedCosmeticCount}</p>
                </div>
                <div className="oa-kpi-card">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Cosmetics</p>
                  <p className="mt-2 text-2xl font-semibold text-oa-text">{profile.equippedCosmeticCount}</p>
                </div>
              </div>

              <section className="oa-panel-soft">
                <p className="text-xs uppercase tracking-[0.16em] text-oa-muted">Current Equipped Loadout</p>
                <div className="mt-3 flex flex-wrap gap-2">
                  {profile.equippedCosmetics.length === 0 ? (
                    <span className="oa-chip">No cosmetics equipped yet.</span>
                  ) : (
                    profile.equippedCosmetics.map((item) => (
                      <span key={item.cosmeticItemId} className="oa-chip oa-chip-route">
                        {formatProfileCategoryLabel(item.category)}: {item.displayName}
                      </span>
                    ))
                  )}
                </div>
              </section>
            </article>
          </section>

          <section className="oa-panel-soft space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <div>
                <p className="text-xs uppercase tracking-[0.16em] text-oa-muted">Loadout Control</p>
                <p className="mt-1 text-sm text-oa-muted">
                  Select a category slot and equip owned cosmetics to update your character instantly.
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  void refresh();
                }}
                className="oa-btn oa-btn-secondary px-3 py-2 text-xs"
                disabled={isLoading || isMutating}
              >
                Refresh
              </button>
            </div>

            <ProfileLoadoutSlots
              categories={PROFILE_CUSTOMIZATION_CATEGORY_ORDER}
              selectedCategory={selectedCategory}
              equippedByCategory={equippedByCategory}
              disabled={isLoading || isMutating}
              onSelect={setSelectedCategory}
            />
          </section>

          <section className="space-y-4">
            <div className="oa-reward-summary-strip">
              <div>
                <p className="text-xs uppercase tracking-[0.16em] text-oa-muted">Customization Options</p>
                <p className="mt-1 text-sm text-oa-text">
                  {formatProfileCategoryLabel(selectedCategory)} cosmetics
                </p>
                <p className="mt-1 text-xs text-oa-muted">
                  Choose an item to equip it to your active {formatProfileCategoryLabel(selectedCategory)} slot.
                </p>
              </div>
              <span className="oa-chip">{itemsInSelectedCategory.length} owned options</span>
            </div>

            <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {itemsInSelectedCategory.length === 0 ? (
                <div className="oa-empty-state">
                  No owned cosmetics in {formatProfileCategoryLabel(selectedCategory)} yet. Visit Store to unlock more style options.
                </div>
              ) : (
                itemsInSelectedCategory.map((item) => (
                  <ProfileOwnedItemCard
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
        </div>
      ) : null}
    </section>
  );
}
