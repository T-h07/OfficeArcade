import { useAuth } from "../../auth/AuthContext";
import { PageHero } from "../../layout/PageHero";
import { AvatarLoadoutPreview } from "../components/AvatarLoadoutPreview";
import { useProfileCustomization } from "../hooks/useProfileCustomization";
import type { ProfileOwnedCosmetic } from "../types/profile.types";
import { PROFILE_CUSTOMIZATION_CATEGORY_ORDER } from "../types/profile.types";

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

function rarityBadgeClass(rarity: ProfileOwnedCosmetic["rarity"]) {
  if (rarity === "EPIC") {
    return "oa-chip-route";
  }
  if (rarity === "RARE") {
    return "oa-chip-info";
  }
  return "";
}

function LoadingState() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="oa-panel h-24" />
      <div className="grid gap-4 xl:grid-cols-[340px_1fr]">
        <div className="oa-panel h-[480px]" />
        <div className="oa-panel h-[480px]" />
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

  if (!user) {
    return null;
  }

  return (
    <section className="oa-page">
      <PageHero
        kicker="Identity & Customization"
        title="Profile Customization"
        subtitle="Tune your avatar loadout from owned inventory items."
        rightSlot={<span className="oa-chip oa-chip-route text-sm font-semibold">Respect: {profile?.respectPoints ?? 0}</span>}
      />

      {isLoading ? <LoadingState /> : null}

      {!isLoading && errorMessage ? (
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

      {!isLoading && !errorMessage && profile ? (
        <div className="space-y-5">
          <section className="grid gap-4 xl:grid-cols-[340px_1fr]">
            <AvatarLoadoutPreview displayName={profile.displayName} layers={profile.avatarLayers} />

            <article className="oa-panel">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-xl font-semibold text-oa-text">{profile.displayName}</h2>
                  <p className="mt-1 text-sm text-oa-muted">{profile.email}</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="oa-chip">
                    {profile.role}
                  </span>
                  <span
                    className={`oa-chip ${profile.department?.active ? "oa-chip-route" : ""}`}
                  >
                    {profile.department
                      ? `${profile.department.displayName} (${profile.department.code})`
                      : "Unassigned Department"}
                  </span>
                  <span
                    className={`oa-chip ${profile.accountEnabled ? "oa-chip-success" : "oa-chip-danger"}`}
                  >
                    {profile.accountEnabled ? "Active" : "Inactive"}
                  </span>
                </div>
              </div>

              <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <div className="oa-panel-soft p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Level</p>
                  <p className="mt-1 text-xl font-semibold text-oa-text">{profile.level}</p>
                </div>
                <div className="oa-panel-soft p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">XP</p>
                  <p className="mt-1 text-xl font-semibold text-oa-text">{profile.xp}</p>
                </div>
                <div className="oa-panel-soft p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Respect</p>
                  <p className="mt-1 text-xl font-semibold text-oa-text">{profile.respectPoints}</p>
                </div>
                <div className="oa-panel-soft p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Karma</p>
                  <p className="mt-1 text-xl font-semibold text-oa-text">{profile.karmaPoints}</p>
                </div>
              </div>

              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <div className="oa-panel-soft p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned Cosmetics</p>
                  <p className="mt-1 text-lg font-semibold text-oa-text">{profile.ownedCosmeticCount}</p>
                </div>
                <div className="oa-panel-soft p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Cosmetics</p>
                  <p className="mt-1 text-lg font-semibold text-oa-text">{profile.equippedCosmeticCount}</p>
                </div>
              </div>

              <div className="oa-panel-soft mt-4 p-3">
                <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Department Placement</p>
                <p className="mt-1 text-sm text-oa-text">
                  {profile.department
                    ? `${profile.department.displayName} (${profile.department.code})`
                    : "Not assigned to a department yet."}
                </p>
                {profile.department && !profile.department.active ? (
                  <p className="mt-1 text-xs text-oa-danger">This department is currently inactive.</p>
                ) : null}
              </div>

              <div className="oa-panel-soft mt-4 p-3">
                <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Current Equipped Loadout</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  {profile.equippedCosmetics.length === 0 ? (
                    <span className="text-sm text-oa-muted">No cosmetics equipped yet.</span>
                  ) : (
                    profile.equippedCosmetics.map((item) => (
                      <span
                        key={item.cosmeticItemId}
                        className="oa-chip"
                      >
                        {item.category}: {item.displayName}
                      </span>
                    ))
                  )}
                </div>
              </div>
            </article>
          </section>

          <section className="oa-panel-soft">
            <div className="flex flex-wrap items-center gap-2">
              {PROFILE_CUSTOMIZATION_CATEGORY_ORDER.map((category) => (
                <button
                  key={category}
                  type="button"
                  onClick={() => setSelectedCategory(category)}
                  className={`oa-chip ${
                    selectedCategory === category
                      ? "oa-chip-route"
                      : ""
                  }`}
                  disabled={isLoading || isMutating}
                >
                  {category}
                </button>
              ))}

              <button
                type="button"
                onClick={() => {
                  void refresh();
                }}
                className="oa-btn oa-btn-secondary ml-auto px-3 py-2 text-xs"
                disabled={isLoading || isMutating}
              >
                Refresh
              </button>
            </div>
          </section>

          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {itemsInSelectedCategory.length === 0 ? (
              <div className="oa-empty-state">
                No owned cosmetics in {selectedCategory}. Purchase items in Store to expand this category.
              </div>
            ) : (
              itemsInSelectedCategory.map((item) => (
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
                  <p className="mt-2 text-xs text-oa-muted">{item.previewAssetKey}</p>

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
                    {!item.enabled ? (
                      <span className="oa-chip oa-chip-danger">
                        Disabled
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
                    disabled={isMutating || isLoading || !item.enabled}
                  >
                    {item.equipped ? "Unequip" : "Equip"}
                  </button>
                </article>
              ))
            )}
          </section>
        </div>
      ) : null}
    </section>
  );
}
