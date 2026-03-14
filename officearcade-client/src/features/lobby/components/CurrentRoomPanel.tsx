import { GameTypeBadge } from "./GameTypeBadge";
import { getGameTypeVisual, getRoomStatusClass } from "./gameTypeVisuals";
import type { LobbyRoomDetail } from "../types/lobby.types";

type CurrentRoomPanelProps = {
  room: LobbyRoomDetail | null;
  currentUserId: string;
  disabled: boolean;
  onLeaveRoom: (roomId: string) => void;
  onCloseRoom: (roomId: string) => void;
  onReportMember: (member: { userId: string; displayName: string; roomId: string }) => void;
};

function formatJoinedAt(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleTimeString();
}

export function CurrentRoomPanel({
  room,
  currentUserId,
  disabled,
  onLeaveRoom,
  onCloseRoom,
  onReportMember
}: CurrentRoomPanelProps) {
  if (!room) {
    return (
      <section className="oa-game-surface">
        <p className="text-xs uppercase tracking-[0.14em] text-oa-muted">Current Staging Room</p>
        <h2 className="mt-1 text-lg font-semibold text-oa-text">No Active Room</h2>
        <p className="mt-2 text-sm text-oa-muted">Create a room or join a session from the browser to start playing.</p>
      </section>
    );
  }

  const visual = getGameTypeVisual(room.gameTypeCode, room.gameTypeDisplayName);
  const isHost = room.hostUserId === currentUserId;
  const capacityPercent = Math.min((room.currentPlayers / Math.max(room.maxPlayers, 1)) * 100, 100);

  return (
    <section className={visual.surfaceClassName}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-oa-muted">Current Staging Room</p>
          <h2 className="mt-1 text-xl font-semibold text-oa-text">{room.roomName}</h2>
          <p className="mt-1 text-sm text-oa-muted">Coordinate players, then launch the match.</p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <GameTypeBadge gameTypeCode={room.gameTypeCode} displayName={room.gameTypeDisplayName} />
          <span className={getRoomStatusClass(room.status)}>{room.status}</span>
          <span className="oa-chip">{room.rounds} rounds</span>
          <span className="oa-live-chip">Staging</span>
        </div>
      </div>

      <div className="mt-3">
        <div className="flex items-center justify-between text-xs text-oa-muted">
          <span>Room capacity</span>
          <span className="text-oa-text">
            {room.currentPlayers}/{room.maxPlayers}
          </span>
        </div>
        <div className="oa-capacity-track mt-1">
          <div className="oa-capacity-fill" style={{ width: `${capacityPercent}%` }} />
        </div>
      </div>

      <div className="mt-4">
        <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Members</p>
        <ul className="mt-2 space-y-2">
          {room.members.map((member) => (
            <li key={member.userId} className="oa-room-card flex items-center justify-between gap-3 p-3">
              <div>
                <p className="font-medium text-oa-text">
                  {member.displayName}
                  {member.userId === currentUserId ? <span className="ml-1 text-xs text-oa-muted">(You)</span> : null}
                </p>
                <p className="text-xs text-oa-muted">Joined {formatJoinedAt(member.joinedAt)}</p>
              </div>
              <div className="flex items-center gap-2">
                <span className={`oa-chip ${member.role === "HOST" ? "oa-chip-route" : ""}`}>{member.role}</span>
                {member.userId !== currentUserId ? (
                  <button
                    type="button"
                    onClick={() => onReportMember({ userId: member.userId, displayName: member.displayName, roomId: room.id })}
                    className="oa-btn oa-btn-ghost px-2.5 py-1 text-xs"
                    disabled={disabled}
                  >
                    Report
                  </button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        <button type="button" onClick={() => onLeaveRoom(room.id)} className="oa-btn oa-btn-secondary px-4 py-2" disabled={disabled}>
          Leave Room
        </button>
        {isHost ? (
          <button type="button" onClick={() => onCloseRoom(room.id)} className="oa-btn oa-btn-danger px-4 py-2" disabled={disabled}>
            Close Room
          </button>
        ) : null}
      </div>

      {isHost ? <p className="mt-3 text-xs text-oa-muted">Host leaving closes the room for all members.</p> : null}
    </section>
  );
}
