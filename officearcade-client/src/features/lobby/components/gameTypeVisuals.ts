export type GameTypeVisual = {
  code: string;
  title: string;
  shortLabel: string;
  iconText: string;
  badgeClassName: string;
  surfaceClassName: string;
  accentClassName: string;
};

const CONNECT_FOUR_VISUAL: GameTypeVisual = {
  code: "CONNECT_FOUR",
  title: "Connect Four",
  shortLabel: "C4",
  iconText: "C4",
  badgeClassName: "oa-game-badge oa-game-connect-four",
  surfaceClassName: "oa-game-surface oa-game-connect-four-surface",
  accentClassName: "oa-game-accent-connect-four"
};

const TRIVIA_VISUAL: GameTypeVisual = {
  code: "TRIVIA",
  title: "Trivia Battle",
  shortLabel: "TRIVIA",
  iconText: "QZ",
  badgeClassName: "oa-game-badge oa-game-trivia",
  surfaceClassName: "oa-game-surface oa-game-trivia-surface",
  accentClassName: "oa-game-accent-trivia"
};

const UNO_VISUAL: GameTypeVisual = {
  code: "UNO",
  title: "UNO-Style",
  shortLabel: "UNO",
  iconText: "UNO",
  badgeClassName: "oa-game-badge oa-game-uno",
  surfaceClassName: "oa-game-surface oa-game-uno-surface",
  accentClassName: "oa-game-accent-uno"
};

const DEFAULT_VISUAL: GameTypeVisual = {
  code: "GENERIC",
  title: "Game",
  shortLabel: "GAME",
  iconText: "GM",
  badgeClassName: "oa-game-badge",
  surfaceClassName: "oa-game-surface",
  accentClassName: ""
};

export function getGameTypeVisual(gameTypeCode: string, displayName?: string): GameTypeVisual {
  const normalized = gameTypeCode.trim().toUpperCase();
  if (normalized === "CONNECT_FOUR") {
    return { ...CONNECT_FOUR_VISUAL, title: displayName ?? CONNECT_FOUR_VISUAL.title };
  }
  if (normalized === "TRIVIA") {
    return { ...TRIVIA_VISUAL, title: displayName ?? TRIVIA_VISUAL.title };
  }
  if (normalized === "UNO") {
    return { ...UNO_VISUAL, title: displayName ?? UNO_VISUAL.title };
  }
  return { ...DEFAULT_VISUAL, title: displayName ?? (normalized || DEFAULT_VISUAL.title) };
}

export function getRoomStatusClass(status: string) {
  const normalized = status.trim().toUpperCase();
  if (normalized === "OPEN") {
    return "oa-chip oa-chip-success";
  }
  if (normalized === "FULL") {
    return "oa-chip oa-chip-warning";
  }
  if (normalized === "CLOSED") {
    return "oa-chip oa-chip-danger";
  }
  return "oa-chip";
}
