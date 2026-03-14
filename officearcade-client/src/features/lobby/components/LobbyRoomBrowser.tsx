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
    return "oa-chip-success";
  }
  if (status === "FULL") {
    return "oa-chip-warning";
  }
  return "";
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
    <section className="oa-panel">
      <div className="flex items-center justify-between gap-2">
        <h2 className="text-lg font-semibold text-oa-text">Room Browser</h2>
        <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">{rooms.length} rooms</p>
      </div>

      <div className="mt-4 space-y-3">
        {playBlocked && blockMessage ? (
          <div className="oa-alert oa-alert-warning">
            {blockMessage}
          </div>
        ) : null}

        {rooms.length === 0 ? (
          <div className="oa-empty-state">
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
                className="oa-action-card"
              >
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div>
                    <h3 className="text-base font-semibold text-oa-text">{room.roomName}</h3>
                    <p className="mt-1 text-sm text-oa-muted">
                      Host: {room.hostDisplayName} · {room.gameTypeDisplayName}
                    </p>
                  </div>

                  <div className="flex flex-wrap gap-2">
                    <span className={`oa-chip ${badgeClass(room.status)}`}>
                      {room.status}
                    </span>
                    <span className="oa-chip">
                      {room.currentPlayers}/{room.maxPlayers} players
                    </span>
                    <span className="oa-chip">
                      {room.rounds} rounds
                    </span>
                    <span className="oa-chip">
                      {room.isPrivate ? "Private" : "Public"}
                    </span>
                  </div>
                </div>

                <div className="mt-3 flex flex-wrap gap-2">
                  {isMyRoom ? (
                    <span className="oa-chip oa-chip-route">
                      You are in this room
                    </span>
                  ) : null}

                  {isHost && !isMyRoom ? (
                    <span className="oa-chip">
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
                    className="oa-btn oa-btn-primary px-3 py-1.5 text-xs"
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
