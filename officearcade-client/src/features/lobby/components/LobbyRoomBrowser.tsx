import { GameTypeBadge } from "./GameTypeBadge";
import { getGameTypeVisual, getRoomStatusClass } from "./gameTypeVisuals";
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

function formatUpdated(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleTimeString();
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
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h2 className="text-lg font-semibold text-oa-text">Live Room Browser</h2>
          <p className="text-xs text-oa-muted">Join open sessions or queue private invites.</p>
        </div>
        <span className="oa-chip">{rooms.length} active rooms</span>
      </div>

      <div className="mt-4 space-y-3">
        {playBlocked && blockMessage ? <div className="oa-alert oa-alert-warning">{blockMessage}</div> : null}

        {rooms.length === 0 ? (
          <div className="oa-empty-state">No open rooms yet. Launch the first session.</div>
        ) : (
          rooms.map((room) => {
            const visual = getGameTypeVisual(room.gameTypeCode, room.gameTypeDisplayName);
            const isMyRoom = room.id === myRoomId;
            const isHost = room.hostUserId === currentUserId;
            const isFull = room.status === "FULL" || room.currentPlayers >= room.maxPlayers;
            const canJoin = !playBlocked && !isMyRoom && !myRoomId && room.status === "OPEN" && !isFull;
            const capacityPercent = Math.min((room.currentPlayers / Math.max(room.maxPlayers, 1)) * 100, 100);
            const cardStateClass = canJoin ? "oa-room-card-joinable" : "oa-room-card-locked";

            return (
              <article key={room.id} className={`oa-room-card ${visual.accentClassName} ${cardStateClass}`}>
                <div className="flex flex-wrap items-start justify-between gap-3">
                  <div className="space-y-1">
                    <p className="text-base font-semibold text-oa-text">{room.roomName}</p>
                    <p className="text-xs text-oa-muted">Host {room.hostDisplayName} · Updated {formatUpdated(room.updatedAt)}</p>
                  </div>

                  <div className="flex flex-wrap items-center gap-2">
                    <GameTypeBadge gameTypeCode={room.gameTypeCode} displayName={room.gameTypeDisplayName} />
                    <span className={getRoomStatusClass(room.status)}>{room.status}</span>
                    <span className="oa-chip">{room.isPrivate ? "Private" : "Public"}</span>
                  </div>
                </div>

                <div className="mt-3 grid gap-2 md:grid-cols-[1fr_auto] md:items-center">
                  <div>
                    <div className="flex items-center justify-between text-xs text-oa-muted">
                      <span>Capacity</span>
                      <span className="text-oa-text">
                        {room.currentPlayers}/{room.maxPlayers} players
                      </span>
                    </div>
                    <div className="oa-capacity-track mt-1">
                      <div className="oa-capacity-fill" style={{ width: `${capacityPercent}%` }} />
                    </div>
                  </div>
                  <div className="flex flex-wrap items-center gap-2 text-xs">
                    <span className="oa-chip">{room.rounds} rounds</span>
                    {isMyRoom ? <span className="oa-chip oa-chip-route">In your room</span> : null}
                    {isHost && !isMyRoom ? <span className="oa-chip">You host</span> : null}
                  </div>
                </div>

                <div className="mt-3 flex flex-wrap items-center justify-between gap-2">
                  <p className="text-xs text-oa-muted">
                    {canJoin ? "Joinable now" : isMyRoom ? "Currently joined" : isFull ? "Room is full" : "Unavailable right now"}
                  </p>
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
                    {room.isPrivate ? "Join Private Room" : "Join Session"}
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
