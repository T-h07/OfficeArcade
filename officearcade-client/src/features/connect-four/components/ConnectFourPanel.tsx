import { useMemo } from "react";
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
      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-oa-text">Connect Four</h2>
          <span className="rounded-full border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-muted">
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
      ? "border-oa-accent/45 bg-oa-accent/15 text-oa-text"
      : gameState.status === "FINISHED"
        ? "border-amber-300/45 bg-amber-300/15 text-amber-100"
        : "border-oa-border bg-black/25 text-oa-muted";

  return (
    <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-oa-text">Connect Four</h2>
          <p className="mt-1 text-sm text-oa-muted">
            Server-authoritative board state for room: <span className="text-oa-text">{room.roomName}</span>
          </p>
        </div>

        <div className="flex flex-wrap gap-2">
          <span className={`rounded-full border px-2.5 py-1 text-xs ${statusChipClass}`}>Status: {statusLabel}</span>
          <span className="rounded-full border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-muted">
            Realtime: {realtimeStatus}
          </span>
        </div>
      </div>

      {errorMessage ? (
        <div className="mt-4 rounded-xl border border-oa-danger/45 bg-oa-danger/10 px-4 py-3 text-sm text-oa-danger">
          {errorMessage}
        </div>
      ) : null}

      {actionMessage ? (
        <div className="mt-4 flex items-center justify-between gap-3 rounded-xl border border-oa-accent/45 bg-oa-accent/10 px-4 py-3">
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

      <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_280px]">
        <div className="rounded-xl border border-oa-border bg-black/20 p-3">
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
                  className="rounded-md border border-oa-border bg-black/25 px-1 py-1 text-[11px] text-oa-muted transition-colors hover:border-oa-accent/45 hover:text-oa-text disabled:cursor-not-allowed disabled:opacity-50"
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

        <aside className="space-y-3 rounded-xl border border-oa-border bg-black/20 p-4">
          <div>
            <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Players</p>
            <div className="mt-2 space-y-2 text-sm">
              <p className="rounded-md border border-oa-border bg-black/30 px-3 py-2 text-oa-text">
                <span className="mr-2 inline-block h-2.5 w-2.5 rounded-full bg-amber-300" />
                {playerOneName}
              </p>
              <p className="rounded-md border border-oa-border bg-black/30 px-3 py-2 text-oa-text">
                <span className="mr-2 inline-block h-2.5 w-2.5 rounded-full bg-sky-400" />
                {playerTwoName}
              </p>
            </div>
          </div>

          <div className="rounded-md border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-muted">
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
              className="rounded-md border border-oa-accent/50 bg-oa-accent/20 px-3 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/30 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={!gameState.canStart || isMutating}
            >
              {gameState.status === "FINISHED" ? "Start New Match" : "Start Match"}
            </button>

            <button
              type="button"
              onClick={() => {
                void refresh();
              }}
              className="rounded-md border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={isMutating || isLoading}
            >
              Refresh Game
            </button>
          </div>

          <p className="text-xs text-oa-muted">
            Move validation, turn order, and match outcome are enforced by the backend. Your client only submits
            actions and renders authoritative state.
          </p>
        </aside>
      </div>

      <div className="mt-3 text-xs text-oa-muted">
        <p>Moves: {gameState.moveCount}</p>
        <p>You are {gameState.playerOneUserId === currentUserId ? "Player One" : gameState.playerTwoUserId === currentUserId ? "Player Two" : "a room member"}.</p>
      </div>

      {gameState.status === "FINISHED" && !gameState.draw && challenge ? (
        <div className="mt-4 rounded-xl border border-oa-border bg-black/20 p-4">
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
