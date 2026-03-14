import { useMemo } from "react";
import { GameTypeBadge } from "../../lobby/components/GameTypeBadge";
import { getGameTypeVisual } from "../../lobby/components/gameTypeVisuals";
import type { LobbyRoomDetail } from "../../lobby/types/lobby.types";
import { useUnoGame } from "../hooks/useUnoGame";
import type { UnoCard, UnoGameState, UnoPlayer } from "../types/uno.types";

type UnoGamePanelProps = {
  accessToken: string | null;
  room: LobbyRoomDetail | null;
  currentUserId: string;
  onUnauthorized: () => void;
};

const UNO_CODE = "UNO";

function findPlayer(state: UnoGameState, userId: string | null): UnoPlayer | null {
  if (!userId) {
    return null;
  }
  return state.players.find((player) => player.userId === userId) ?? null;
}

function colorBadgeClass(color: UnoCard["color"] | null) {
  if (color === "RED") {
    return "border-rose-300/50 bg-rose-400/20 text-rose-100";
  }
  if (color === "YELLOW") {
    return "border-amber-300/50 bg-amber-300/20 text-amber-100";
  }
  if (color === "GREEN") {
    return "border-emerald-300/50 bg-emerald-400/20 text-emerald-100";
  }
  if (color === "BLUE") {
    return "border-sky-300/50 bg-sky-400/20 text-sky-100";
  }
  return "border-oa-border bg-black/25 text-oa-muted";
}

function cardClass(card: UnoCard, playable: boolean) {
  const base =
    "group relative rounded-lg border px-3 py-2 text-left transition-transform duration-150 disabled:cursor-not-allowed disabled:opacity-45";
  const playableStyles = playable
    ? "hover:-translate-y-1 hover:scale-[1.02] border-white/35 shadow-[0_0_0_1px_rgba(255,255,255,0.08)_inset]"
    : "border-white/10";

  if (card.color === "RED") {
    return `${base} ${playableStyles} bg-gradient-to-b from-rose-500/75 to-rose-700/80 text-white`;
  }
  if (card.color === "YELLOW") {
    return `${base} ${playableStyles} bg-gradient-to-b from-amber-300/90 to-amber-500/85 text-slate-900`;
  }
  if (card.color === "GREEN") {
    return `${base} ${playableStyles} bg-gradient-to-b from-emerald-400/85 to-emerald-700/85 text-white`;
  }
  return `${base} ${playableStyles} bg-gradient-to-b from-sky-400/85 to-sky-700/85 text-white`;
}

function actionText(card: UnoCard) {
  if (card.type === "NUMBER") {
    return `Number ${card.label}`;
  }
  return card.label;
}

export function UnoGamePanel({ accessToken, room, currentUserId, onUnauthorized }: UnoGamePanelProps) {
  const isUnoRoom = room?.gameTypeCode === UNO_CODE;
  const visual = getGameTypeVisual(UNO_CODE, "UNO-Style");

  const {
    gameState,
    isLoading,
    isMutating,
    realtimeStatus,
    errorMessage,
    actionMessage,
    refresh,
    startGame,
    playCard,
    drawCard,
    clearActionMessage
  } = useUnoGame(accessToken, room?.id ?? null, isUnoRoom, onUnauthorized);

  const statusLabel = useMemo(() => {
    if (!gameState) {
      return "Initializing";
    }
    if (gameState.status === "WAITING") {
      return "Waiting";
    }
    if (gameState.status === "ACTIVE") {
      return "In Progress";
    }
    return "Finished";
  }, [gameState]);

  if (!room || !isUnoRoom) {
    return null;
  }

  if (isLoading || !gameState) {
    return (
      <section className={visual.surfaceClassName}>
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <GameTypeBadge gameTypeCode={UNO_CODE} displayName="UNO-Style" />
            <h2 className="text-lg font-semibold text-oa-text">Card Sync</h2>
          </div>
          <span className={realtimeStatus === "connected" ? "oa-live-chip" : "oa-chip"}>
            Realtime: {realtimeStatus}
          </span>
        </div>
        <p className="mt-3 text-sm text-oa-muted">{isLoading ? "Loading game state..." : "Preparing game state..."}</p>
      </section>
    );
  }

  const statusChipClass =
    gameState.status === "ACTIVE"
      ? "oa-chip oa-chip-success"
      : gameState.status === "FINISHED"
        ? "oa-chip oa-chip-warning"
        : "oa-chip";

  const playableTokens = new Set(gameState.playableCardTokens);
  const winner = findPlayer(gameState, gameState.winnerUserId);
  const turnPlayer = findPlayer(gameState, gameState.currentTurnUserId);
  const isFinished = gameState.status === "FINISHED";

  return (
    <section className={visual.surfaceClassName}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <GameTypeBadge gameTypeCode={UNO_CODE} displayName="UNO-Style" />
            <h2 className="text-lg font-semibold text-oa-text">Card Table</h2>
          </div>
          <p className="mt-1 text-sm text-oa-muted">
            Card-driven turn play in <span className="text-oa-text">{room.roomName}</span>.
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <span className={statusChipClass}>Status: {statusLabel}</span>
          <span className={realtimeStatus === "connected" ? "oa-live-chip" : "oa-chip"}>
            Realtime: {realtimeStatus}
          </span>
        </div>
      </div>

      {errorMessage ? (
        <div className="oa-alert oa-alert-danger mt-4">
          {errorMessage}
        </div>
      ) : null}

      {actionMessage ? (
        <div className="oa-alert oa-alert-success mt-4 flex items-center justify-between gap-3">
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

      <div className="mt-4 grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="oa-panel-soft space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Table State</p>
            <span className="oa-chip">
              Direction: {gameState.direction === "CLOCKWISE" ? "Clockwise" : "Counterclockwise"}
            </span>
          </div>

          <div className="grid gap-3 sm:grid-cols-2">
            <div className="oa-panel-soft p-3">
              <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Top Discard</p>
              {gameState.topDiscardCard ? (
                <div className="oa-room-card mt-2 p-3">
                  <span className={`inline-flex rounded-full border px-2 py-0.5 text-xs ${colorBadgeClass(gameState.topDiscardCard.color)}`}>
                    {gameState.topDiscardCard.color}
                  </span>
                  <p className="mt-2 text-base font-semibold text-oa-text">{actionText(gameState.topDiscardCard)}</p>
                </div>
              ) : (
                <p className="mt-2 text-sm text-oa-muted">No discard card yet.</p>
              )}
            </div>

            <div className="oa-panel-soft space-y-3 p-3">
              <div>
                <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Current Color</p>
                <span className={`mt-2 inline-flex rounded-full border px-2.5 py-1 text-xs ${colorBadgeClass(gameState.currentColor)}`}>
                  {gameState.currentColor ?? "None"}
                </span>
              </div>
              <div className="grid grid-cols-2 gap-2 text-sm">
                <p className="rounded-md border border-oa-border bg-black/25 px-2 py-1 text-oa-muted">
                  Draw: <span className="text-oa-text">{gameState.drawPileCount}</span>
                </p>
                <p className="rounded-md border border-oa-border bg-black/25 px-2 py-1 text-oa-muted">
                  Discard: <span className="text-oa-text">{gameState.discardPileCount}</span>
                </p>
              </div>
              <button
                type="button"
                onClick={() => {
                  void drawCard();
                }}
                className="oa-btn oa-btn-secondary w-full px-3 py-2"
                disabled={!gameState.canDraw || isMutating || isFinished}
              >
                Draw One Card
              </button>
            </div>
          </div>

          <div className="oa-panel-soft p-3">
            <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">My Hand ({gameState.myHand.length})</p>
            {gameState.myHand.length === 0 ? (
              <p className="mt-2 text-sm text-oa-muted">{isFinished ? "You have no cards left." : "No cards dealt yet."}</p>
            ) : (
              <div className="mt-2 grid gap-2 sm:grid-cols-3 xl:grid-cols-4">
                {gameState.myHand.map((card) => {
                  const playable = gameState.myTurn && playableTokens.has(card.token) && !isFinished;
                  return (
                    <button
                      key={card.token}
                      type="button"
                      onClick={() => {
                        void playCard(card.token);
                      }}
                      className={cardClass(card, playable)}
                      disabled={!playable || isMutating}
                    >
                      <p className="text-[11px] uppercase tracking-[0.12em] opacity-90">{card.color}</p>
                      <p className="mt-1 text-sm font-semibold">{card.label}</p>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        <aside className="oa-panel-soft space-y-3">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Players</p>
          <div className="space-y-2">
            {gameState.players.map((player) => (
              <div
                key={player.userId}
                className={`oa-room-card px-3 py-2 ${
                  player.currentTurn
                    ? "border-oa-accent/45 bg-oa-accent/10"
                    : ""
                }`}
              >
                <div className="flex items-center justify-between gap-2">
                  <p className="text-sm font-medium text-oa-text">
                    {player.displayName}
                    {player.userId === currentUserId ? " (you)" : ""}
                  </p>
                  <span className="rounded-full border border-oa-border bg-black/25 px-2 py-0.5 text-xs text-oa-muted">
                    {player.handCount} cards
                  </span>
                </div>
                <p className="mt-1 text-xs text-oa-muted">{player.currentTurn ? "Current turn" : "Waiting"}</p>
              </div>
            ))}
          </div>

          <div className="oa-room-card px-3 py-2 text-sm text-oa-muted">
            {gameState.status === "WAITING" ? (
              <p>Waiting for host start. UNO supports 2 to 4 players.</p>
            ) : null}
            {gameState.status === "ACTIVE" ? (
              <p>
                Turn: <span className="font-medium text-oa-text">{turnPlayer?.displayName ?? "Unknown"}</span>
                {gameState.myTurn ? " (your turn)" : ""}
              </p>
            ) : null}
            {gameState.status === "FINISHED" ? (
              <p>
                Winner: <span className="font-medium text-oa-text">{winner?.displayName ?? "No winner"}</span>
              </p>
            ) : null}
          </div>

          <div className="flex flex-col gap-2">
            <button
              type="button"
              onClick={() => {
                void startGame();
              }}
              className="oa-btn oa-btn-primary px-3 py-2"
              disabled={!gameState.canStart || isMutating}
            >
              {gameState.status === "FINISHED" ? "Start New UNO Match" : "Start UNO Match"}
            </button>
            <button
              type="button"
              onClick={() => {
                void refresh();
              }}
              className="oa-btn oa-btn-secondary px-3 py-2"
              disabled={isMutating || isLoading}
            >
              Refresh Game
            </button>
          </div>

          <p className="text-xs text-oa-muted">
            Rule scope: numbers, Skip, Reverse, and Draw Two.
          </p>
          <p className="text-xs text-oa-muted">Moves played: {gameState.moveCount}</p>
        </aside>
      </div>
    </section>
  );
}
