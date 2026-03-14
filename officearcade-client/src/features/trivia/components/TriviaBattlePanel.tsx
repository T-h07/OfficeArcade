import { useMemo } from "react";
import { GameTypeBadge } from "../../lobby/components/GameTypeBadge";
import { getGameTypeVisual } from "../../lobby/components/gameTypeVisuals";
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
  const visual = getGameTypeVisual(TRIVIA_CODE, "Trivia Battle");

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
      <section className={visual.surfaceClassName}>
        <div className="flex items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <GameTypeBadge gameTypeCode={TRIVIA_CODE} displayName="Trivia Battle" />
            <h2 className="text-lg font-semibold text-oa-text">Round Sync</h2>
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

  const winnerName = findPlayerName(gameState, gameState.winnerUserId);
  const canAnswer = gameState.status === "ACTIVE" && gameState.canAnswer && !isMutating;
  const currentQuestion = gameState.currentQuestion;
  const lastRound = gameState.lastRoundOutcome;

  return (
    <section className={visual.surfaceClassName}>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <GameTypeBadge gameTypeCode={TRIVIA_CODE} displayName="Trivia Battle" />
            <h2 className="text-lg font-semibold text-oa-text">Match Table</h2>
          </div>
          <p className="mt-1 text-sm text-oa-muted">
            Timed question rounds in <span className="text-oa-text">{room.roomName}</span>.
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
        <div className="oa-panel-soft space-y-3">
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
            <div className="oa-empty-state">
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
                    className="oa-room-card px-3 py-2 text-left text-sm text-oa-text"
                    disabled={!canAnswer}
                  >
                    <span className="mr-2 text-oa-muted">{String.fromCharCode(65 + option.index)}.</span>
                    {option.label}
                  </button>
                ))}
              </div>

              {gameState.waitingForOpponent ? (
                <p className="oa-empty-state">
                  Answer locked in. Waiting for the other player.
                </p>
              ) : null}
            </div>
          ) : null}

          {gameState.status === "FINISHED" ? (
            <div className="oa-panel-soft p-4">
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
            <div className="oa-panel-soft p-4">
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

        <aside className="oa-panel-soft space-y-3">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Scoreboard</p>
          <div className="space-y-2">
            {gameState.players.map((player) => (
              <div key={player.userId} className="oa-room-card px-3 py-2">
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
            Question progression and score resolution are backend authoritative.
          </p>
        </aside>
      </div>
    </section>
  );
}
