import type { ProfileAvatarLayer } from "../types/profile.types";

type AvatarLoadoutPreviewProps = {
  displayName: string;
  layers: ProfileAvatarLayer[];
};

function findLayer(layers: ProfileAvatarLayer[], category: ProfileAvatarLayer["category"]) {
  return layers.find((layer) => layer.category === category);
}

function resolveFrameColor(assetKey: string | undefined) {
  if (assetKey === "frame.executive") {
    return { border: "#f8d48a", glow: "rgba(248, 212, 138, 0.35)" };
  }
  if (assetKey === "frame.neon") {
    return { border: "#39e0ff", glow: "rgba(57, 224, 255, 0.35)" };
  }
  return { border: "#3c4f70", glow: "rgba(80, 111, 156, 0.28)" };
}

function resolveOutfitStyle(assetKey: string | undefined) {
  if (assetKey === "outfit.blazer-pro") {
    return {
      background: "linear-gradient(180deg, #253753 0%, #1a2943 55%, #152137 100%)",
      border: "1px solid rgba(196, 214, 243, 0.35)"
    };
  }
  if (assetKey === "outfit.hoodie-casual") {
    return {
      background: "linear-gradient(180deg, #47607c 0%, #354a63 55%, #2b3e57 100%)",
      border: "1px solid rgba(174, 198, 232, 0.28)"
    };
  }
  return {
    background: "linear-gradient(180deg, #3d4f69 0%, #2f4159 55%, #24354b 100%)",
    border: "1px solid rgba(174, 198, 232, 0.25)"
  };
}

function resolveAccessoryColor(assetKey: string | undefined) {
  if (assetKey === "accessory.gold-pin") {
    return "#f8ce7a";
  }
  if (assetKey === "accessory.stripe-tie") {
    return "#2bc4a9";
  }
  return "#9db2d6";
}

function resolveHatColors(assetKey: string | undefined) {
  if (assetKey === "hat.nightshift-beanie") {
    return { top: "#2a3144", bottom: "#36425a" };
  }
  if (assetKey === "hat.classic-cap") {
    return { top: "#415470", bottom: "#2c3a4f" };
  }
  return { top: "#3e4f6a", bottom: "#31415a" };
}

function resolveGlassesColor(assetKey: string | undefined) {
  if (assetKey === "glasses.focus-mode") {
    return "#7db8ff";
  }
  if (assetKey === "glasses.deskframe") {
    return "#9fb6d8";
  }
  return "#95aecf";
}

function resolveBadgeColors(assetKey: string | undefined) {
  if (assetKey === "badge.team-player") {
    return { bg: "#306284", border: "#65b8ff", text: "#d9f1ff", label: "TEAM" };
  }
  if (assetKey === "badge.coffee-club") {
    return { bg: "#5b4b2e", border: "#d4ac63", text: "#fcefd7", label: "COFFEE" };
  }
  return { bg: "#2f3a4f", border: "#8ea2c0", text: "#dce6f5", label: "BADGE" };
}

export function AvatarLoadoutPreview({ displayName, layers }: AvatarLoadoutPreviewProps) {
  const frameLayer = findLayer(layers, "PROFILE_FRAME");
  const outfitLayer = findLayer(layers, "OUTFIT");
  const accessoryLayer = findLayer(layers, "ACCESSORY");
  const glassesLayer = findLayer(layers, "GLASSES");
  const hatLayer = findLayer(layers, "HAT");
  const badgeLayer = findLayer(layers, "BADGE");

  const frameColor = resolveFrameColor(frameLayer?.previewAssetKey);
  const outfitStyle = resolveOutfitStyle(outfitLayer?.previewAssetKey);
  const accessoryColor = resolveAccessoryColor(accessoryLayer?.previewAssetKey);
  const glassesColor = resolveGlassesColor(glassesLayer?.previewAssetKey);
  const hatColors = resolveHatColors(hatLayer?.previewAssetKey);
  const badgeColors = resolveBadgeColors(badgeLayer?.previewAssetKey);

  return (
    <section className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-oa-muted">Avatar Preview</p>
          <h2 className="mt-1 text-lg font-semibold text-oa-text">{displayName}</h2>
          <p className="mt-1 text-sm text-oa-muted">Layered render from equipped cosmetic loadout.</p>
        </div>
      </div>

      <div className="mt-5 flex justify-center">
        <div className="relative aspect-[3/4] w-full max-w-[290px]">
          <div
            className="absolute inset-0 rounded-[26px] border-2"
            style={{
              borderColor: frameColor.border,
              boxShadow: `0 0 34px ${frameColor.glow}`
            }}
          />
          <div className="absolute inset-[10px] rounded-[20px] bg-gradient-to-b from-[#111d2f] via-[#0f1b2c] to-[#0b1422]" />

          <div className="absolute left-1/2 top-[3.8rem] h-20 w-20 -translate-x-1/2 rounded-full border border-[#8da7cb]/60 bg-[#f0caa2]" />

          <div
            className="absolute left-1/2 top-[7.1rem] h-44 w-40 -translate-x-1/2 rounded-[30px_30px_16px_16px]"
            style={outfitStyle}
          />

          {accessoryLayer ? (
            <div className="absolute left-1/2 top-[8.5rem] h-28 w-10 -translate-x-1/2">
              <div
                className="absolute left-1/2 top-0 h-4 w-4 -translate-x-1/2 rotate-45 rounded-[4px]"
                style={{ backgroundColor: accessoryColor }}
              />
              <div
                className="absolute left-1/2 top-[0.72rem] h-16 w-3 -translate-x-1/2 rounded-b-md"
                style={{ backgroundColor: accessoryColor }}
              />
            </div>
          ) : null}

          {glassesLayer ? (
            <div className="absolute left-1/2 top-[5.85rem] h-6 w-20 -translate-x-1/2">
              <div
                className="absolute left-0 top-0 h-6 w-8 rounded-full border-2 bg-black/15"
                style={{ borderColor: glassesColor }}
              />
              <div
                className="absolute right-0 top-0 h-6 w-8 rounded-full border-2 bg-black/15"
                style={{ borderColor: glassesColor }}
              />
              <div className="absolute left-1/2 top-[0.65rem] h-[2px] w-4 -translate-x-1/2 bg-[#9ab2d3]" />
            </div>
          ) : null}

          {hatLayer ? (
            <>
              <div
                className="absolute left-1/2 top-[2.35rem] h-8 w-24 -translate-x-1/2 rounded-[16px_16px_8px_8px]"
                style={{ backgroundColor: hatColors.top }}
              />
              <div
                className="absolute left-1/2 top-[4.05rem] h-3 w-28 -translate-x-1/2 rounded-full"
                style={{ backgroundColor: hatColors.bottom }}
              />
            </>
          ) : null}

          {badgeLayer ? (
            <div
              className="absolute bottom-4 right-4 rounded-full border px-3 py-1 text-[10px] font-semibold tracking-wide"
              style={{
                backgroundColor: badgeColors.bg,
                borderColor: badgeColors.border,
                color: badgeColors.text
              }}
            >
              {badgeColors.label}
            </div>
          ) : null}
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {layers
          .filter((layer) => layer.source === "COSMETIC")
          .map((layer) => (
            <span
              key={layer.layerKey}
              className="rounded-full border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-muted"
            >
              {layer.category}
            </span>
          ))}
      </div>
    </section>
  );
}
