import { useMemo } from "react";
import type { LobbyRoomDetail } from "../../lobby/types/lobby.types";
import { useTriviaGame } from "../hooks/useTriviaGame";
import type { TriviaGameState } from "../types/trivia.types";

type TriviaBattlePanelProps = {
  accessToken: string | null;
  room: LobbyRoomDetail | null;
  currentUserId: string;
  onUnauthorized: () => void;
};

const TRIVIA_CODE = "TRIVIA";

function findPlayerName(state: TriviaGameState, userId: string | null) {
  if (!userId) {
    return "Unknown";
  }
  return state.players.find((player) => player.userId === userId)?.displayName ?? "Unknown";
}

export function TriviaBattlePanel({ accessToken, room, currentUserId, onUnauthorized }: TriviaBattlePanelProps) {
  const isTriviaRoom = room?.gameTypeCode === TRIVIA_CODE;

  const {
    gameState,
    isLoading,
    isMutating,
    realtimeStatus,
    errorMessage,
    actionMessage,
    refresh,
    startGame,
    submitAnswer,
    clearActionMessage
  } = useTriviaGame(accessToken, room?.id ?? null, isTriviaRoom, onUnauthorized);

  const statusLabel = useMemo(() => {
    if (!gameState) {
      return "Initializing";
    }
    if (gameState.status === "WAITING") {
      return "Waiting";
    }
    if (gameState.status === "ACTIVE") {
      return "Active";
    }
    return "Finished";
  }, [gameState]);

  if (!room || !isTriviaRoom) {
    return null;
  }

  if (isLoading || !gameState) {
    return (
      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
        <div className="flex items-center justify-between gap-3">
          <h2 className="text-lg font-semibold text-oa-text">Trivia Battle</h2>
          <span className="rounded-full border border-oa-border bg-black/25 px-2.5 py-1 text-xs text-oa-muted">
            Realtime: {realtimeStatus}
          </span>
        </div>
        <p className="mt-3 text-sm text-oa-muted">{isLoading ? "Loading game state..." : "Preparing game state..."}</p>
      </section>
    );
  }

  const statusChipClass =
    gameState.status === "ACTIVE"
      ? "border-oa-accent/45 bg-oa-accent/15 text-oa-text"
      : gameState.status === "FINISHED"
        ? "border-amber-300/45 bg-amber-300/15 text-amber-100"
        : "border-oa-border bg-black/25 text-oa-muted";

  const winnerName = findPlayerName(gameState, gameState.winnerUserId);
  const canAnswer = gameState.status === "ACTIVE" && gameState.canAnswer && !isMutating;
  const currentQuestion = gameState.currentQuestion;
  const lastRound = gameState.lastRoundOutcome;

  return (
    <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-oa-text">Trivia Battle</h2>
          <p className="mt-1 text-sm text-oa-muted">
            Live synchronized rounds for room: <span className="text-oa-text">{room.roomName}</span>
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
        <div className="space-y-3 rounded-xl border border-oa-border bg-black/20 p-4">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">
              Round {Math.max(gameState.currentRound, 0)} / {gameState.totalRounds}
            </p>
            {gameState.status === "ACTIVE" ? (
              <p className="text-xs text-oa-muted">
                {gameState.answeredByCurrentUser ? "Answer submitted" : "Choose one answer"}
              </p>
            ) : null}
          </div>

          {gameState.status === "WAITING" ? (
            <div className="rounded-lg border border-oa-border bg-black/25 p-4 text-sm text-oa-muted">
              Waiting for host to start Trivia Battle. Exactly 2 players are required.
            </div>
          ) : null}

          {gameState.status === "ACTIVE" && currentQuestion ? (
            <div className="space-y-3">
              <div className="rounded-lg border border-oa-border bg-black/25 p-4">
                <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">
                  {currentQuestion.category} · {currentQuestion.difficulty}
                </p>
                <h3 className="mt-2 text-base font-semibold text-oa-text">{currentQuestion.prompt}</h3>
              </div>

              <div className="grid gap-2">
                {currentQuestion.options.map((option) => (
                  <button
                    key={option.index}
                    type="button"
                    onClick={() => {
                      void submitAnswer(option.index);
                    }}
                    className="rounded-lg border border-oa-border bg-black/25 px-3 py-2 text-left text-sm text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={!canAnswer}
                  >
                    <span className="mr-2 text-oa-muted">{String.fromCharCode(65 + option.index)}.</span>
                    {option.label}
                  </button>
                ))}
              </div>

              {gameState.waitingForOpponent ? (
                <p className="rounded-md border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-muted">
                  Answer locked in. Waiting for the other player.
                </p>
              ) : null}
            </div>
          ) : null}

          {gameState.status === "FINISHED" ? (
            <div className="rounded-lg border border-oa-border bg-black/25 p-4">
              {gameState.draw ? (
                <p className="text-sm text-oa-text">Match result: Draw.</p>
              ) : (
                <p className="text-sm text-oa-text">
                  Winner: <span className="font-semibold">{winnerName}</span>
                </p>
              )}
            </div>
          ) : null}

          {lastRound ? (
            <div className="rounded-lg border border-oa-border bg-black/25 p-4">
              <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Last Round Result</p>
              <p className="mt-1 text-sm text-oa-text">
                Round {lastRound.roundNumber}: {lastRound.questionPrompt}
              </p>
              <p className="mt-1 text-xs text-oa-muted">
                Correct option: {String.fromCharCode(65 + lastRound.correctOptionIndex)}
              </p>
              <ul className="mt-2 space-y-1">
                {lastRound.playerAnswers.map((answer) => (
                  <li key={answer.userId} className="text-sm text-oa-muted">
                    <span className="text-oa-text">{answer.displayName}</span>:{" "}
                    {String.fromCharCode(65 + answer.selectedOptionIndex)}{" "}
                    <span className={answer.correct ? "text-oa-accent" : "text-oa-danger"}>
                      {answer.correct ? "correct" : "incorrect"}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>

        <aside className="space-y-3 rounded-xl border border-oa-border bg-black/20 p-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Scoreboard</p>
          <div className="space-y-2">
            {gameState.players.map((player) => (
              <div key={player.userId} className="rounded-md border border-oa-border bg-black/25 px-3 py-2">
                <div className="flex items-center justify-between gap-2">
                  <p className="text-sm font-medium text-oa-text">
                    {player.displayName}
                    {player.userId === currentUserId ? " (you)" : ""}
                  </p>
                  <span className="text-base font-semibold text-oa-text">{player.score}</span>
                </div>
                <p className="mt-1 text-xs text-oa-muted">
                  {player.answeredCurrentRound ? "Answered current round" : "Waiting to answer"}
                </p>
              </div>
            ))}
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
            The backend controls question progression, answer validation, and score calculation. The client only
            submits choices and renders authoritative game state.
          </p>
        </aside>
      </div>
    </section>
  );
}
