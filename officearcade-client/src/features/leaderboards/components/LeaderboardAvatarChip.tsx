type LeaderboardAvatarChipProps = {
  displayName: string;
  profileFrameAssetKey: string | null;
  badgeAssetKey: string | null;
};

function initialsOf(name: string) {
  const tokens = name
    .split(" ")
    .map((token) => token.trim())
    .filter((token) => token.length > 0);
  if (tokens.length === 0) {
    return "OA";
  }
  if (tokens.length === 1) {
    return tokens[0].slice(0, 2).toUpperCase();
  }
  return `${tokens[0][0]}${tokens[1][0]}`.toUpperCase();
}

function resolveFrameStyle(assetKey: string | null) {
  if (assetKey === "frame.executive") {
    return { borderColor: "#f8d48a", boxShadow: "0 0 14px rgba(248, 212, 138, 0.28)" };
  }
  if (assetKey === "frame.neon") {
    return { borderColor: "#39e0ff", boxShadow: "0 0 14px rgba(57, 224, 255, 0.32)" };
  }
  return { borderColor: "#3b4f6f", boxShadow: "none" };
}

function resolveBadgeStyle(assetKey: string | null) {
  if (assetKey === "badge.team-player") {
    return { label: "TP", bg: "#306284", border: "#65b8ff", text: "#d9f1ff" };
  }
  if (assetKey === "badge.coffee-club") {
    return { label: "CF", bg: "#5b4b2e", border: "#d4ac63", text: "#fcefd7" };
  }
  return { label: "BD", bg: "#2f3a4f", border: "#8ea2c0", text: "#dce6f5" };
}

export function LeaderboardAvatarChip({
  displayName,
  profileFrameAssetKey,
  badgeAssetKey
}: LeaderboardAvatarChipProps) {
  const frameStyle = resolveFrameStyle(profileFrameAssetKey);
  const badgeStyle = resolveBadgeStyle(badgeAssetKey);

  return (
    <div className="relative inline-flex h-11 w-11 items-center justify-center">
      <div
        className="flex h-full w-full items-center justify-center rounded-full border-2 bg-gradient-to-b from-[#2a4161] to-[#1a2a42] text-[11px] font-semibold text-oa-text"
        style={{
          ...frameStyle,
          boxShadow: `${frameStyle.boxShadow}, inset 0 0 0 1px rgba(255,255,255,0.06)`
        }}
      >
        {initialsOf(displayName)}
      </div>
      <div className="pointer-events-none absolute inset-0 rounded-full border border-white/10" />
      {badgeAssetKey ? (
        <span
          className="absolute -bottom-1 -right-1 rounded-full border px-1.5 py-0.5 text-[9px] font-semibold"
          style={{
            backgroundColor: badgeStyle.bg,
            borderColor: badgeStyle.border,
            color: badgeStyle.text
          }}
        >
          {badgeStyle.label}
        </span>
      ) : null}
    </div>
  );
}
