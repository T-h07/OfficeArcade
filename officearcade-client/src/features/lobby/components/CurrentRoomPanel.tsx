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
      <section className="oa-panel">
        <h2 className="text-lg font-semibold text-oa-text">Your Current Room</h2>
        <p className="mt-2 text-sm text-oa-muted">
          You are not currently in a room. Create a room or join an available one.
        </p>
      </section>
    );
  }

  const isHost = room.hostUserId === currentUserId;

  return (
    <section className="oa-panel">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-oa-text">Your Current Room</h2>
          <p className="mt-1 text-sm text-oa-muted">
            {room.roomName} · {room.gameTypeDisplayName}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="oa-chip">
            {room.status}
          </span>
          <span className="oa-chip">
            {room.currentPlayers}/{room.maxPlayers} players
          </span>
          <span className="oa-chip">
            {room.rounds} rounds
          </span>
        </div>
      </div>

      <div className="oa-panel-soft mt-4">
        <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Members</p>
        <ul className="mt-2 space-y-2">
          {room.members.map((member) => (
            <li
              key={member.userId}
              className="oa-action-card flex items-center justify-between px-3 py-2 text-sm"
            >
              <div>
                <p className="font-medium text-oa-text">{member.displayName}</p>
                <p className="text-xs text-oa-muted">Joined {formatJoinedAt(member.joinedAt)}</p>
              </div>
              <div className="flex items-center gap-2">
                <span className="oa-chip">
                  {member.role}
                </span>
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
        <button
          type="button"
          onClick={() => onLeaveRoom(room.id)}
          className="oa-btn oa-btn-secondary px-4 py-2"
          disabled={disabled}
        >
          Leave Room
        </button>
        {isHost ? (
          <button
            type="button"
            onClick={() => onCloseRoom(room.id)}
            className="oa-btn oa-btn-danger px-4 py-2"
            disabled={disabled}
          >
            Close Room
          </button>
        ) : null}
      </div>

      {isHost ? (
        <p className="mt-3 text-xs text-oa-muted">
          Host rule: leaving as host closes the room for everyone.
        </p>
      ) : null}
    </section>
  );
}
