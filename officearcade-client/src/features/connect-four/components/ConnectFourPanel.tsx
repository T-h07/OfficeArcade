import { useMemo } from "react";
import { GameTypeBadge } from "../../lobby/components/GameTypeBadge";
import { getGameTypeVisual } from "../../lobby/components/gameTypeVisuals";
import { useConnectFourGame } from "../hooks/useConnectFourGame";
import type { ConnectFourGameState } from "../types/connectFour.types";
import type { LobbyRoomDetail } from "../../lobby/types/lobby.types";

type ConnectFourPanelProps = {
  accessToken: string | null;
  room: LobbyRoomDetail | null;
  currentUserId: string;
  onUnauthorized: () => void;
};

const CONNECT_FOUR_CODE = "CONNECT_FOUR";

function tokenClass(token: number) {
  if (token === 1) {
    return "bg-amber-300 shadow-[0_0_0_1px_rgba(255,255,255,0.15)_inset]";
  }
  if (token === 2) {
    return "bg-sky-400 shadow-[0_0_0_1px_rgba(255,255,255,0.2)_inset]";
  }
  return "bg-black/35";
}

function findPlayerName(state: ConnectFourGameState, userId: string | null) {
  if (!userId) {
    return "Unknown";
  }
  return state.players.find((player) => player.userId === userId)?.displayName ?? "Unknown";
}

export function ConnectFourPanel({ accessToken, room, currentUserId, onUnauthorized }: ConnectFourPanelProps) {
  const isConnectFourRoom = room?.gameTypeCode === CONNECT_FOUR_CODE;
  const visual = getGameTypeVisual(CONNECT_FOUR_CODE, "Connect Four");

  const {
    gameState,
    isLoading,
    isMutating,
    realtimeStatus,
    errorMessage,
    actionMessage,
    refresh,
    startGame,
    makeMove,
    clearActionMessage
  } = useConnectFourGame(accessToken, room?.id ?? null, isConnectFourRoom, onUnauthorized);

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

  if (!room || !isConnectFourRoom) {
    return null;
  }

  if (isLoading || !gameState) {
    return (
      <section className={visual.surfaceClassName}>
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <GameTypeBadge gameTypeCode={CONNECT_FOUR_CODE} displayName="Connect Four" />
            <h2 className="text-lg font-semibold text-oa-text">Board Sync</h2>
          </div>
          <span className={realtimeStatus === "connected" ? "oa-live-chip" : "oa-chip"}>
            Realtime: {realtimeStatus}
          </span>
        </div>
        <p className="mt-3 text-sm text-oa-muted">{isLoading ? "Loading game state..." : "Preparing game state..."}</p>
      </section>
    );
  }

  const playerOneName = findPlayerName(gameState, gameState.playerOneUserId);
  const playerTwoName = findPlayerName(gameState, gameState.playerTwoUserId);
  const currentTurnName = findPlayerName(gameState, gameState.currentTurnUserId);
  const winnerName = findPlayerName(gameState, gameState.winnerUserId);
  const challenge = gameState.challenge;
  const currentUserIsObligated = challenge?.obligatedUserId === currentUserId;
  const currentUserIsBeneficiary = challenge?.beneficiaryUserId === currentUserId;

  const statusChipClass =
    gameState.status === "ACTIVE"
      ? "oa-chip oa-chip-success"
      : gameState.status === "FINISHED"
        ? "oa-chip oa-chip-warning"
        : "oa-chip";

  return (
    <section className={visual.surfaceClassName}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <GameTypeBadge gameTypeCode={CONNECT_FOUR_CODE} displayName="Connect Four" />
            <h2 className="text-lg font-semibold text-oa-text">Match Table</h2>
          </div>
          <p className="mt-1 text-sm text-oa-muted">
            Tactical board play in <span className="text-oa-text">{room.roomName}</span>.
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

      <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_280px]">
        <div className="oa-panel-soft p-3">
          <div className="grid grid-cols-7 gap-1.5 pb-2">
            {Array.from({ length: gameState.columns }, (_, column) => {
              const canDrop =
                gameState.status === "ACTIVE" &&
                gameState.canMove &&
                !isMutating &&
                gameState.board[0]?.[column] === 0;
              return (
                <button
                  key={`drop-${column}`}
                  type="button"
                  onClick={() => {
                    void makeMove(column);
                  }}
                  className="oa-btn oa-btn-ghost px-1 py-1 text-[11px]"
                  disabled={!canDrop}
                >
                  Drop
                </button>
              );
            })}
          </div>

          <div className="grid grid-cols-7 gap-1.5 rounded-lg border border-oa-border bg-[#0b1224] p-2">
            {gameState.board.flatMap((row, rowIndex) =>
              row.map((token, colIndex) => (
                <div
                  key={`cell-${rowIndex}-${colIndex}`}
                  className={`aspect-square rounded-full border border-black/40 ${tokenClass(token)}`}
                />
              ))
            )}
          </div>
        </div>

        <aside className="oa-panel-soft space-y-3">
          <div>
            <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Players</p>
            <div className="mt-2 space-y-2 text-sm">
              <p className="oa-room-card px-3 py-2 text-oa-text">
                <span className="mr-2 inline-block h-2.5 w-2.5 rounded-full bg-amber-300" />
                {playerOneName}
              </p>
              <p className="oa-room-card px-3 py-2 text-oa-text">
                <span className="mr-2 inline-block h-2.5 w-2.5 rounded-full bg-sky-400" />
                {playerTwoName}
              </p>
            </div>
          </div>

          <div className="oa-room-card px-3 py-2 text-sm text-oa-muted">
            {gameState.status === "WAITING" ? (
              <p>Waiting for host to start. Exactly 2 players are required.</p>
            ) : null}
            {gameState.status === "ACTIVE" ? (
              <p>
                Turn: <span className="font-medium text-oa-text">{currentTurnName}</span>
                {gameState.myTurn ? " (your turn)" : ""}
              </p>
            ) : null}
            {gameState.status === "FINISHED" && gameState.draw ? (
              <p>Match result: Draw.</p>
            ) : null}
            {gameState.status === "FINISHED" && !gameState.draw ? (
              <p>
                Winner: <span className="font-medium text-oa-text">{winnerName}</span>
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
              {gameState.status === "FINISHED" ? "Start New Match" : "Start Match"}
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
            Turn order and move validation are backend authoritative.
          </p>
        </aside>
      </div>

      <div className="mt-3 text-xs text-oa-muted">
        <p>Moves: {gameState.moveCount}</p>
        <p>You are {gameState.playerOneUserId === currentUserId ? "Player One" : gameState.playerTwoUserId === currentUserId ? "Player Two" : "a room member"}.</p>
      </div>

      {gameState.status === "FINISHED" && !gameState.draw && challenge ? (
        <div className="oa-panel-soft mt-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Post-match challenge</p>
          <h3 className="mt-1 text-base font-semibold text-oa-text">{challenge.challengeTypeDisplayName}</h3>
          <p className="mt-1 text-sm text-oa-muted">
            Status: <span className="font-medium text-oa-text">{challenge.status}</span>
          </p>
          <p className="mt-2 text-sm text-oa-muted">
            {currentUserIsBeneficiary
              ? "You are the confirmer. Resolve this in the Challenges page."
              : currentUserIsObligated
                ? "You are the obligated player for this office-safe challenge."
                : "Challenge created for this completed match."}
          </p>
          <p className="mt-2 text-xs text-oa-muted">
            Open the <span className="text-oa-text">Challenges</span> page to confirm or reject fulfillment.
          </p>
        </div>
      ) : null}
    </section>
  );
}
