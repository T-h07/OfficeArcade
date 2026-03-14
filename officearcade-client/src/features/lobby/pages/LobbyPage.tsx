import { useState } from "react";
import { ConnectFourPanel } from "../../connect-four/components/ConnectFourPanel";
import { usePlayLimits } from "../../play-limits/hooks/usePlayLimits";
import { TriviaBattlePanel } from "../../trivia/components/TriviaBattlePanel";
import { UnoGamePanel } from "../../uno/components/UnoGamePanel";
import { useAuth } from "../../auth/AuthContext";
import { ReportUserModal } from "../../moderation/components/ReportUserModal";
import { ModerationApiError, submitModerationReport } from "../../moderation/api/moderationApi";
import type { CreateModerationReportRequest } from "../../moderation/types/moderation.types";
import { CreateRoomForm } from "../components/CreateRoomForm";
import { CurrentRoomPanel } from "../components/CurrentRoomPanel";
import { JoinPrivateRoomModal } from "../components/JoinPrivateRoomModal";
import { LobbyRoomBrowser } from "../components/LobbyRoomBrowser";
import { useLobby } from "../hooks/useLobby";

type PrivateJoinTarget = {
  roomId: string;
  roomName: string;
} | null;

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

function formatDuration(seconds: number) {
  const safe = Math.max(seconds, 0);
  const hours = Math.floor(safe / 3600);
  const minutes = Math.floor((safe % 3600) / 60);
  const remainingSeconds = safe % 60;

  if (hours > 0) {
    return `${hours}h ${minutes}m`;
  }
  if (minutes > 0) {
    return `${minutes}m ${remainingSeconds}s`;
  }
  return `${remainingSeconds}s`;
}

export function LobbyPage() {
  const { accessToken, logout, user } = useAuth();
  const [privateJoinTarget, setPrivateJoinTarget] = useState<PrivateJoinTarget>(null);
  const [reportTarget, setReportTarget] = useState<{ userId: string; displayName: string; roomId: string } | null>(null);
  const [isSubmittingReport, setIsSubmittingReport] = useState(false);
  const [reportError, setReportError] = useState<string | null>(null);
  const {
    summary: playLimits,
    isLoading: isPlayLimitsLoading,
    errorMessage: playLimitsError,
    cooldownRemainingSecondsLive,
    refresh: refreshPlayLimits
  } = usePlayLimits(accessToken, logout);

  const {
    rooms,
    gameTypes,
    myRoom,
    isLoading,
    isMutating,
    realtimeStatus,
    errorMessage,
    actionMessage,
    refresh,
    createRoom,
    joinRoom,
    leaveRoom,
    closeRoom,
    clearActionMessage
  } = useLobby(accessToken, logout);

  if (!user) {
    return null;
  }

  const playBlocked = Boolean(playLimits && !playLimits.canPlayNow);
  const blockedByCooldown = playLimits?.eligibilityReason === "COOLDOWN_ACTIVE";
  const blockedByDailyLimit = playLimits?.eligibilityReason === "DAILY_LIMIT_REACHED";
  const blockedMessage = playBlocked
    ? blockedByCooldown
      ? `You are on cooldown. Come back in ${formatDuration(cooldownRemainingSecondsLive)}.`
      : "Daily play limit reached. New games unlock after the daily reset."
    : null;

  const realtimeBadgeClass =
    realtimeStatus === "connected"
      ? "border-oa-accent/45 bg-oa-accent/15 text-oa-text"
      : realtimeStatus === "connecting"
        ? "border-amber-300/45 bg-amber-300/15 text-amber-100"
        : "border-oa-border bg-black/25 text-oa-muted";

  async function handleJoinPublic(roomId: string) {
    if (playBlocked) {
      return;
    }
    await joinRoom(roomId, {});
  }

  async function handleLeaveRoom(roomId: string) {
    await leaveRoom(roomId);
  }

  async function handleCloseRoom(roomId: string) {
    await closeRoom(roomId);
  }

  async function handleSubmitRoomMemberReport(request: CreateModerationReportRequest) {
    if (!accessToken) {
      return;
    }
    setIsSubmittingReport(true);
    setReportError(null);
    try {
      await submitModerationReport(accessToken, request);
      setReportTarget(null);
    } catch (error) {
      if (error instanceof ModerationApiError && error.status === 401) {
        logout();
        return;
      }
      setReportError(error instanceof Error ? error.message : "Unable to submit report.");
    } finally {
      setIsSubmittingReport(false);
    }
  }

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Multiplayer Foundation</p>
        <div className="mt-2 flex flex-wrap items-center justify-between gap-2">
          <h1 className="text-2xl font-semibold text-oa-text">Play Lobby</h1>
          <span className={`rounded-full border px-2.5 py-1 text-xs ${realtimeBadgeClass}`}>
            Realtime: {realtimeStatus === "connected" ? "Live" : realtimeStatus}
          </span>
        </div>
        <p className="mt-2 text-sm text-oa-muted">
          Create and join persisted room sessions with live lobby and room sync across clients.
        </p>

        <div className="mt-4 flex flex-wrap gap-2">
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
            Daily Limit: {playLimits?.dailyGameLimit ?? "-"}
          </span>
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
            Played Today: {playLimits?.gamesPlayedToday ?? "-"}
          </span>
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
            Remaining: {playLimits?.gamesRemainingToday ?? "-"}
          </span>
          <span
            className={`rounded-full border px-2.5 py-1 text-xs ${
              playBlocked
                ? "border-amber-300/45 bg-amber-300/15 text-amber-100"
                : "border-oa-accent/45 bg-oa-accent/15 text-oa-text"
            }`}
          >
            Eligibility: {playLimits ? (playBlocked ? playLimits.eligibilityReason : "ELIGIBLE") : "Loading"}
          </span>
        </div>
      </header>

      {isPlayLimitsLoading ? (
        <div className="rounded-xl border border-oa-border bg-black/20 px-4 py-3 text-sm text-oa-muted">
          Loading play-limit status...
        </div>
      ) : null}

      {playLimitsError ? (
        <div className="rounded-xl border border-oa-danger/45 bg-oa-danger/10 px-4 py-3 text-sm text-oa-danger">
          {playLimitsError}
        </div>
      ) : null}

      {playBlocked && playLimits ? (
        <div className="rounded-2xl border border-amber-300/45 bg-amber-300/10 p-5">
          <p className="text-xs uppercase tracking-[0.12em] text-amber-100">Play Restricted</p>
          <h2 className="mt-2 text-xl font-semibold text-oa-text">
            {blockedByCooldown ? "You’re On Cooldown" : "Daily Play Limit Reached"}
          </h2>
          <p className="mt-2 text-sm text-amber-100">{blockedMessage}</p>
          <p className="mt-2 text-sm text-oa-text">
            Games remaining today: <span className="font-semibold">{playLimits.gamesRemainingToday}</span>
          </p>
          {blockedByCooldown ? (
            <p className="mt-1 text-sm text-oa-text">
              Next playable time: <span className="font-semibold">{formatDateTime(playLimits.cooldownUntil)}</span>
            </p>
          ) : null}
          {blockedByDailyLimit ? (
            <p className="mt-1 text-sm text-oa-text">
              Daily reset at: <span className="font-semibold">{formatDateTime(playLimits.nextDailyResetAt)}</span>
            </p>
          ) : null}
          <button
            type="button"
            onClick={refreshPlayLimits}
            className="mt-4 rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/50"
          >
            Recheck Eligibility
          </button>
        </div>
      ) : null}

      {errorMessage ? (
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

      <CurrentRoomPanel
        room={myRoom}
        currentUserId={user.id}
        disabled={isMutating}
        onLeaveRoom={handleLeaveRoom}
        onCloseRoom={handleCloseRoom}
        onReportMember={(member) => {
          setReportError(null);
          setReportTarget(member);
        }}
      />

      <ConnectFourPanel accessToken={accessToken} room={myRoom} currentUserId={user.id} onUnauthorized={logout} />
      <TriviaBattlePanel accessToken={accessToken} room={myRoom} currentUserId={user.id} onUnauthorized={logout} />
      <UnoGamePanel accessToken={accessToken} room={myRoom} currentUserId={user.id} onUnauthorized={logout} />

      <div className="grid gap-5 xl:grid-cols-[380px_1fr]">
        <CreateRoomForm
          gameTypes={gameTypes}
          disabled={isMutating || isLoading || playBlocked}
          onSubmit={async (request) => {
            if (playBlocked) {
              return false;
            }
            return createRoom(request);
          }}
        />

        <LobbyRoomBrowser
          rooms={rooms}
          currentUserId={user.id}
          myRoomId={myRoom?.id ?? null}
          isBusy={isMutating || isLoading}
          playBlocked={playBlocked}
          blockMessage={blockedMessage}
          onJoinPublic={(roomId) => {
            void handleJoinPublic(roomId);
          }}
          onJoinPrivate={(roomId, roomName) => {
            setPrivateJoinTarget({ roomId, roomName });
          }}
        />
      </div>

      <div className="flex justify-end">
        <button
          type="button"
          onClick={() => {
            void refresh();
          }}
          className="rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/50 disabled:cursor-not-allowed disabled:opacity-65"
          disabled={isLoading || isMutating}
        >
          {isLoading ? "Refreshing..." : "Refresh Lobby"}
        </button>
      </div>

      <JoinPrivateRoomModal
        isOpen={privateJoinTarget !== null}
        roomName={privateJoinTarget?.roomName ?? ""}
        isSubmitting={isMutating}
        onClose={() => setPrivateJoinTarget(null)}
        onSubmit={async (password) => {
          if (!privateJoinTarget) {
            return false;
          }
          if (playBlocked) {
            return false;
          }
          return joinRoom(privateJoinTarget.roomId, { password });
        }}
      />

      <ReportUserModal
        isOpen={reportTarget !== null}
        reportedUserId={reportTarget?.userId ?? ""}
        reportedDisplayName={reportTarget?.displayName ?? ""}
        context={
          reportTarget
            ? {
                sourceRoomId: reportTarget.roomId
              }
            : undefined
        }
        isSubmitting={isSubmittingReport}
        errorMessage={reportError}
        onClose={() => {
          if (isSubmittingReport) {
            return;
          }
          setReportError(null);
          setReportTarget(null);
        }}
        onSubmit={handleSubmitRoomMemberReport}
      />
    </section>
  );
}
