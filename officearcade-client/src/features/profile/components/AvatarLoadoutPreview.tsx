import { useEffect, useMemo, useState } from "react";
import type { CosmeticCategory } from "../../store/types/store.types";
import type { ProfileAvatarLayer } from "../types/profile.types";
import { initialLettersOf } from "./profilePresentation";

type AvatarLoadoutPreviewProps = {
  displayName: string;
  layers: ProfileAvatarLayer[];
  selectedCategory?: CosmeticCategory;
};

function findLayer(layers: ProfileAvatarLayer[], category: ProfileAvatarLayer["category"]) {
  return layers.find((layer) => layer.category === category);
}

function resolveFrameColor(assetKey: string | undefined) {
  if (assetKey === "frame.executive") {
    return { border: "#f8d48a", glow: "rgba(248, 212, 138, 0.4)", ambient: "248,212,138" };
  }
  if (assetKey === "frame.neon") {
    return { border: "#39e0ff", glow: "rgba(57, 224, 255, 0.44)", ambient: "57,224,255" };
  }
  return { border: "#5f7ab0", glow: "rgba(95, 122, 176, 0.35)", ambient: "111,173,255" };
}

function resolveOutfitStyle(assetKey: string | undefined) {
  if (assetKey === "outfit.blazer-pro") {
    return {
      background: "linear-gradient(180deg, #2d4263 0%, #213251 54%, #18263e 100%)",
      border: "1px solid rgba(196, 214, 243, 0.42)"
    };
  }
  if (assetKey === "outfit.hoodie-casual") {
    return {
      background: "linear-gradient(180deg, #5a7392 0%, #415877 55%, #334968 100%)",
      border: "1px solid rgba(174, 198, 232, 0.34)"
    };
  }
  return {
    background: "linear-gradient(180deg, #465d7c 0%, #33496a 55%, #273b58 100%)",
    border: "1px solid rgba(174, 198, 232, 0.3)"
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
    return { top: "#2e3550", bottom: "#404d67" };
  }
  if (assetKey === "hat.classic-cap") {
    return { top: "#4f6587", bottom: "#354866" };
  }
  return { top: "#435877", bottom: "#334867" };
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

function layerSignature(layers: ProfileAvatarLayer[]) {
  return layers
    .filter((layer) => layer.source === "COSMETIC")
    .map((layer) => `${layer.category}:${layer.previewAssetKey}`)
    .sort()
    .join("|");
}

const COSMETIC_ORDER: Array<ProfileAvatarLayer["category"]> = [
  "OUTFIT",
  "ACCESSORY",
  "GLASSES",
  "HAT",
  "PROFILE_FRAME",
  "BADGE"
];

export function AvatarLoadoutPreview({ displayName, layers, selectedCategory }: AvatarLoadoutPreviewProps) {
  const [pulse, setPulse] = useState(false);

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
  const signature = useMemo(() => layerSignature(layers), [layers]);

  useEffect(() => {
    setPulse(true);
    const timeoutId = window.setTimeout(() => setPulse(false), 280);
    return () => window.clearTimeout(timeoutId);
  }, [signature]);

  return (
    <section className="oa-character-stage-shell">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Character Stage</p>
          <h2 className="mt-1 text-xl font-semibold text-oa-text">{displayName}</h2>
          <p className="mt-1 text-sm text-oa-muted">Your equipped cosmetics are layered into a live arcade identity preview.</p>
        </div>
        <div className="flex items-center gap-2">
          <span className="oa-chip">{initialLettersOf(displayName)}</span>
          {selectedCategory ? <span className="oa-chip oa-chip-route">Editing: {selectedCategory}</span> : null}
        </div>
      </div>

      <div className={`oa-character-stage mt-5 ${pulse ? "oa-character-stage-pulse" : ""}`}>
        <div
          className="oa-character-stage-backdrop"
          style={{
            background: `radial-gradient(130% 110% at 50% -10%, rgba(${frameColor.ambient},0.28), rgba(${frameColor.ambient},0) 56%), linear-gradient(180deg, #101a2e 0%, #0d1728 58%, #0b1322 100%)`
          }}
        />
        <div
          className="oa-character-stage-ring"
          style={{
            borderColor: frameColor.border,
            boxShadow: `0 0 40px ${frameColor.glow}`
          }}
        />
        <div className="oa-character-floor" />

        <div className="oa-character-model">
          <div className="oa-character-shadow" />
          <div
            className="oa-character-body"
            style={outfitStyle}
          />
          <div className="oa-character-neck" />
          <div className="oa-character-head" />
          <div className="oa-character-hairline" />

          {accessoryLayer ? (
            <div className="oa-character-accessory">
              {accessoryLayer.previewAssetKey === "accessory.gold-pin" ? (
                <div className="oa-character-pin" style={{ backgroundColor: accessoryColor }} />
              ) : (
                <div className="oa-character-tie">
                  <div className="oa-character-tie-knot" style={{ backgroundColor: accessoryColor }} />
                  <div className="oa-character-tie-drop" style={{ backgroundColor: accessoryColor }} />
                </div>
              )}
            </div>
          ) : null}

          {glassesLayer ? (
            <div className="oa-character-glasses">
              <div className="oa-character-glasses-lens" style={{ borderColor: glassesColor }} />
              <div className="oa-character-glasses-lens" style={{ borderColor: glassesColor }} />
              <div className="oa-character-glasses-bridge" />
            </div>
          ) : null}

          {hatLayer ? (
            <div className="oa-character-hat">
              <div className="oa-character-hat-top" style={{ backgroundColor: hatColors.top }} />
              <div className="oa-character-hat-brim" style={{ backgroundColor: hatColors.bottom }} />
            </div>
          ) : null}

          {badgeLayer ? (
            <div
              className="oa-character-badge"
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

      <div className="mt-4 grid gap-2 sm:grid-cols-2">
        {COSMETIC_ORDER.map((category) => {
          const layer = findLayer(layers, category);
          return (
            <div
              key={category}
              className={`rounded-lg border px-3 py-2 text-xs ${
                selectedCategory === category
                  ? "border-[rgba(var(--oa-route-rgb),0.62)] bg-[rgba(var(--oa-route-rgb),0.16)]"
                  : "border-oa-border/40 bg-oa-surface-soft/38"
              }`}
            >
              <p className="uppercase tracking-[0.12em] text-oa-muted">{category}</p>
              <p className="mt-1 text-sm font-semibold text-oa-text">{layer?.displayName ?? "Empty Slot"}</p>
            </div>
          );
        })}
      </div>

      <div className="mt-3 flex flex-wrap gap-2">
        {layers
          .filter((layer) => layer.source === "COSMETIC")
          .map((layer) => (
            <span
              key={layer.layerKey}
              className={`oa-chip ${selectedCategory === layer.category ? "oa-chip-route" : ""}`}
            >
              {layer.category}
            </span>
          ))}
      </div>
    </section>
  );
}
