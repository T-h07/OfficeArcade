import type { LobbyRoomSummary } from "../types/lobby.types";

type LobbyRoomBrowserProps = {
  rooms: LobbyRoomSummary[];
  currentUserId: string;
  myRoomId: string | null;
  isBusy: boolean;
  playBlocked: boolean;
  blockMessage: string | null;
  onJoinPublic: (roomId: string) => void;
  onJoinPrivate: (roomId: string, roomName: string) => void;
};

function badgeClass(status: LobbyRoomSummary["status"]) {
  if (status === "OPEN") {
    return "border-oa-accent/45 bg-oa-accent/15 text-oa-text";
  }
  if (status === "FULL") {
    return "border-amber-300/45 bg-amber-300/15 text-amber-100";
  }
  return "border-oa-border bg-black/25 text-oa-muted";
}

export function LobbyRoomBrowser({
  rooms,
  currentUserId,
  myRoomId,
  isBusy,
  playBlocked,
  blockMessage,
  onJoinPublic,
  onJoinPrivate
}: LobbyRoomBrowserProps) {
  return (
    <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-semibold text-oa-text">Room Browser</h2>
        <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">{rooms.length} rooms</p>
      </div>

      <div className="mt-4 space-y-3">
        {playBlocked && blockMessage ? (
          <div className="rounded-xl border border-amber-300/45 bg-amber-300/10 px-4 py-3 text-sm text-amber-100">
            {blockMessage}
          </div>
        ) : null}

        {rooms.length === 0 ? (
          <div className="rounded-xl border border-oa-border bg-black/20 px-4 py-5 text-sm text-oa-muted">
            No open rooms yet. Host the first one.
          </div>
        ) : (
          rooms.map((room) => {
            const isMyRoom = room.id === myRoomId;
            const isHost = room.hostUserId === currentUserId;
            const isFull = room.status === "FULL" || room.currentPlayers >= room.maxPlayers;
            const canJoin = !playBlocked && !isMyRoom && !myRoomId && room.status === "OPEN" && !isFull;

            return (
              <article
                key={room.id}
                className="rounded-xl border border-oa-border bg-oa-surface-soft/55 p-4 transition-colors hover:border-oa-accent/35"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h3 className="text-base font-semibold text-oa-text">{room.roomName}</h3>
                    <p className="mt-1 text-sm text-oa-muted">
                      Host: {room.hostDisplayName} · {room.gameTypeDisplayName}
                    </p>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <span className={`rounded-full border px-2.5 py-1 text-xs ${badgeClass(room.status)}`}>
                      {room.status}
                    </span>
                    <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
                      {room.currentPlayers}/{room.maxPlayers} players
                    </span>
                    <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
                      {room.rounds} rounds
                    </span>
                    <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
                      {room.isPrivate ? "Private" : "Public"}
                    </span>
                  </div>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  {isMyRoom ? (
                    <span className="rounded-md border border-oa-accent/50 bg-oa-accent/15 px-3 py-1.5 text-xs font-medium text-oa-text">
                      You are in this room
                    </span>
                  ) : null}

                  {isHost && !isMyRoom ? (
                    <span className="rounded-md border border-oa-border bg-black/20 px-3 py-1.5 text-xs text-oa-muted">
                      You are the host
                    </span>
                  ) : null}

                  <button
                    type="button"
                    onClick={() => {
                      if (room.isPrivate) {
                        onJoinPrivate(room.id, room.roomName);
                      } else {
                        onJoinPublic(room.id);
                      }
                    }}
                    className="rounded-md border border-oa-accent/50 bg-oa-accent/20 px-3 py-1.5 text-xs font-semibold text-oa-text transition-colors hover:bg-oa-accent/30 disabled:cursor-not-allowed disabled:opacity-60"
                    disabled={!canJoin || isBusy}
                  >
                    {room.isPrivate ? "Join (Password)" : "Join Room"}
                  </button>
                </div>
              </article>
            );
          })
        )}
      </div>
    </section>
  );
}
