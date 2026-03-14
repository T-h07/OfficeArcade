import { useAuth } from "../../auth/AuthContext";
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
    return "border-fuchsia-300/45 bg-fuchsia-300/15 text-fuchsia-100";
  }
  if (rarity === "RARE") {
    return "border-sky-300/45 bg-sky-300/15 text-sky-100";
  }
  return "border-oa-border bg-black/20 text-oa-muted";
}

function LoadingState() {
  return (
    <div className="space-y-4 animate-pulse">
      <div className="h-24 rounded-2xl border border-oa-border bg-oa-surface/70" />
      <div className="grid gap-4 xl:grid-cols-[340px_1fr]">
        <div className="h-[480px] rounded-2xl border border-oa-border bg-oa-surface/70" />
        <div className="h-[480px] rounded-2xl border border-oa-border bg-oa-surface/70" />
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
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Identity & Customization</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-semibold text-oa-text">Profile Customization</h1>
          <span className="rounded-full border border-oa-accent/45 bg-oa-accent/15 px-3 py-1 text-sm font-semibold text-oa-text">
            Respect: {profile?.respectPoints ?? 0}
          </span>
        </div>
        <p className="mt-2 text-sm text-oa-muted">
          Equipped cosmetics are rendered as a layered avatar loadout and persist directly from your account inventory.
        </p>
      </header>

      {isLoading ? <LoadingState /> : null}

      {!isLoading && errorMessage ? (
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

      {!isLoading && !errorMessage && profile ? (
        <div className="space-y-5">
          <section className="grid gap-4 xl:grid-cols-[340px_1fr]">
            <AvatarLoadoutPreview displayName={profile.displayName} layers={profile.avatarLayers} />

            <article className="rounded-2xl border border-oa-border bg-oa-surface/82 p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-xl font-semibold text-oa-text">{profile.displayName}</h2>
                  <p className="mt-1 text-sm text-oa-muted">{profile.email}</p>
                </div>
                <div className="flex items-center gap-2">
                  <span className="rounded-full border border-oa-border bg-black/20 px-3 py-1 text-xs font-medium text-oa-text">
                    {profile.role}
                  </span>
                  <span
                    className={`rounded-full border px-3 py-1 text-xs font-medium ${
                      profile.accountEnabled
                        ? "border-oa-accent/45 bg-oa-accent/15 text-oa-text"
                        : "border-oa-danger/45 bg-oa-danger/15 text-oa-danger"
                    }`}
                  >
                    {profile.accountEnabled ? "Active" : "Inactive"}
                  </span>
                </div>
              </div>

              <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
                <div className="rounded-lg border border-oa-border bg-black/20 p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Level</p>
                  <p className="mt-1 text-xl font-semibold text-oa-text">{profile.level}</p>
                </div>
                <div className="rounded-lg border border-oa-border bg-black/20 p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">XP</p>
                  <p className="mt-1 text-xl font-semibold text-oa-text">{profile.xp}</p>
                </div>
                <div className="rounded-lg border border-oa-border bg-black/20 p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Respect</p>
                  <p className="mt-1 text-xl font-semibold text-oa-text">{profile.respectPoints}</p>
                </div>
                <div className="rounded-lg border border-oa-border bg-black/20 p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Karma</p>
                  <p className="mt-1 text-xl font-semibold text-oa-text">{profile.karmaPoints}</p>
                </div>
              </div>

              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <div className="rounded-lg border border-oa-border bg-black/20 p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Owned Cosmetics</p>
                  <p className="mt-1 text-lg font-semibold text-oa-text">{profile.ownedCosmeticCount}</p>
                </div>
                <div className="rounded-lg border border-oa-border bg-black/20 p-3">
                  <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Equipped Cosmetics</p>
                  <p className="mt-1 text-lg font-semibold text-oa-text">{profile.equippedCosmeticCount}</p>
                </div>
              </div>

              <div className="mt-4 rounded-lg border border-oa-border bg-black/20 p-3">
                <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Current Equipped Loadout</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  {profile.equippedCosmetics.length === 0 ? (
                    <span className="text-sm text-oa-muted">No cosmetics equipped yet.</span>
                  ) : (
                    profile.equippedCosmetics.map((item) => (
                      <span
                        key={item.cosmeticItemId}
                        className="rounded-full border border-oa-border bg-oa-surface-soft/70 px-2.5 py-1 text-xs text-oa-text"
                      >
                        {item.category}: {item.displayName}
                      </span>
                    ))
                  )}
                </div>
              </div>
            </article>
          </section>

          <section className="rounded-2xl border border-oa-border bg-oa-surface/82 p-4">
            <div className="flex flex-wrap items-center gap-2">
              {PROFILE_CUSTOMIZATION_CATEGORY_ORDER.map((category) => (
                <button
                  key={category}
                  type="button"
                  onClick={() => setSelectedCategory(category)}
                  className={`rounded-full border px-3 py-1.5 text-xs font-medium transition-colors ${
                    selectedCategory === category
                      ? "border-oa-accent/55 bg-oa-accent/15 text-oa-text"
                      : "border-oa-border bg-black/20 text-oa-muted hover:border-oa-accent/45 hover:text-oa-text"
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
                className="ml-auto rounded-md border border-oa-border bg-black/25 px-3 py-2 text-xs text-oa-text transition-colors hover:border-oa-accent/50 disabled:cursor-not-allowed disabled:opacity-60"
                disabled={isLoading || isMutating}
              >
                Refresh
              </button>
            </div>
          </section>

          <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {itemsInSelectedCategory.length === 0 ? (
              <div className="rounded-xl border border-oa-border bg-oa-surface/75 p-5 text-sm text-oa-muted">
                No owned cosmetics in {selectedCategory}. Purchase items in Store to expand this category.
              </div>
            ) : (
              itemsInSelectedCategory.map((item) => (
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
                  <p className="mt-2 text-xs text-oa-muted">{item.previewAssetKey}</p>

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
                    {!item.enabled ? (
                      <span className="rounded-full border border-oa-danger/45 bg-oa-danger/15 px-2.5 py-1 text-xs text-oa-danger">
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
                    className="mt-4 w-full rounded-md border border-oa-accent/50 bg-oa-accent/20 px-3 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/30 disabled:cursor-not-allowed disabled:opacity-60"
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
