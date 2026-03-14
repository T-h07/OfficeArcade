import { useState } from "react";
import { useAuth } from "../../auth/AuthContext";
import { CreateRoomForm } from "../components/CreateRoomForm";
import { CurrentRoomPanel } from "../components/CurrentRoomPanel";
import { JoinPrivateRoomModal } from "../components/JoinPrivateRoomModal";
import { LobbyRoomBrowser } from "../components/LobbyRoomBrowser";
import { useLobby } from "../hooks/useLobby";

type PrivateJoinTarget = {
  roomId: string;
  roomName: string;
} | null;

export function LobbyPage() {
  const { accessToken, logout, user } = useAuth();
  const [privateJoinTarget, setPrivateJoinTarget] = useState<PrivateJoinTarget>(null);

  const {
    rooms,
    gameTypes,
    myRoom,
    isLoading,
    isMutating,
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

  async function handleJoinPublic(roomId: string) {
    await joinRoom(roomId, {});
  }

  async function handleLeaveRoom(roomId: string) {
    await leaveRoom(roomId);
  }

  async function handleCloseRoom(roomId: string) {
    await closeRoom(roomId);
  }

  return (
    <section className="space-y-5">
      <header className="rounded-2xl border border-oa-border bg-oa-surface/85 p-5">
        <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">Multiplayer Foundation</p>
        <h1 className="mt-2 text-2xl font-semibold text-oa-text">Play Lobby</h1>
        <p className="mt-2 text-sm text-oa-muted">
          Create and join persisted room sessions. Realtime orchestration arrives in OA-PT07.
        </p>
      </header>

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
      />

      <div className="grid gap-5 xl:grid-cols-[380px_1fr]">
        <CreateRoomForm gameTypes={gameTypes} disabled={isMutating || isLoading} onSubmit={createRoom} />

        <LobbyRoomBrowser
          rooms={rooms}
          currentUserId={user.id}
          myRoomId={myRoom?.id ?? null}
          isBusy={isMutating || isLoading}
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
          return joinRoom(privateJoinTarget.roomId, { password });
        }}
      />
    </section>
  );
}
