import { useState } from "react";
import { ConnectFourPanel } from "../../connect-four/components/ConnectFourPanel";
import { usePlayLimits } from "../../play-limits/hooks/usePlayLimits";
import { TriviaBattlePanel } from "../../trivia/components/TriviaBattlePanel";
import { UnoGamePanel } from "../../uno/components/UnoGamePanel";
import { useAuth } from "../../auth/AuthContext";
import { ReportUserModal } from "../../moderation/components/ReportUserModal";
import { ModerationApiError, submitModerationReport } from "../../moderation/api/moderationApi";
import type { CreateModerationReportRequest } from "../../moderation/types/moderation.types";
import { PageHero } from "../../layout/PageHero";
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
      ? "oa-chip-success"
      : realtimeStatus === "connecting"
        ? "oa-chip-warning"
        : "";

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
    <section className="oa-page">
      <PageHero
        kicker="Multiplayer Arena"
        title="Play Lobby"
        subtitle="Create or join a room and launch short synchronized matches."
        rightSlot={
          <span className={`oa-chip ${realtimeBadgeClass}`}>
            Realtime: {realtimeStatus === "connected" ? "Live" : realtimeStatus}
          </span>
        }
        footerSlot={
          <>
            <span className="oa-chip">Daily Limit: {playLimits?.dailyGameLimit ?? "-"}</span>
            <span className="oa-chip">Played Today: {playLimits?.gamesPlayedToday ?? "-"}</span>
            <span className="oa-chip">Remaining: {playLimits?.gamesRemainingToday ?? "-"}</span>
            <span className={`oa-chip ${playBlocked ? "oa-chip-warning" : "oa-chip-success"}`}>
              Eligibility: {playLimits ? (playBlocked ? playLimits.eligibilityReason : "ELIGIBLE") : "Loading"}
            </span>
          </>
        }
      />

      {isPlayLimitsLoading ? (
        <div className="oa-empty-state">
          Loading play-limit status...
        </div>
      ) : null}

      {playLimitsError ? (
        <div className="oa-alert oa-alert-danger">
          {playLimitsError}
        </div>
      ) : null}

      {playBlocked && playLimits ? (
        <div className="oa-panel">
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
            className="oa-btn oa-btn-secondary mt-4 px-3 py-2"
          >
            Recheck Eligibility
          </button>
        </div>
      ) : null}

      {errorMessage ? (
        <div className="oa-alert oa-alert-danger">
          {errorMessage}
        </div>
      ) : null}

      {actionMessage ? (
        <div className="oa-alert oa-alert-success flex items-center justify-between gap-3">
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
          className="oa-btn oa-btn-secondary px-3 py-2"
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
