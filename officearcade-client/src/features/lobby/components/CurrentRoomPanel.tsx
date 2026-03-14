import type { LobbyRoomDetail } from "../types/lobby.types";

type CurrentRoomPanelProps = {
  room: LobbyRoomDetail | null;
  currentUserId: string;
  disabled: boolean;
  onLeaveRoom: (roomId: string) => void;
  onCloseRoom: (roomId: string) => void;
};

function formatJoinedAt(value: string) {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return value;
  }
  return parsed.toLocaleTimeString();
}

export function CurrentRoomPanel({ room, currentUserId, disabled, onLeaveRoom, onCloseRoom }: CurrentRoomPanelProps) {
  if (!room) {
    return (
      <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
        <h2 className="text-lg font-semibold text-oa-text">Your Current Room</h2>
        <p className="mt-2 text-sm text-oa-muted">
          You are not currently in a room. Create a room or join an available one.
        </p>
      </section>
    );
  }

  const isHost = room.hostUserId === currentUserId;

  return (
    <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h2 className="text-lg font-semibold text-oa-text">Your Current Room</h2>
          <p className="mt-1 text-sm text-oa-muted">
            {room.roomName} · {room.gameTypeDisplayName}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
            {room.status}
          </span>
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
            {room.currentPlayers}/{room.maxPlayers} players
          </span>
          <span className="rounded-full border border-oa-border bg-black/20 px-2.5 py-1 text-xs text-oa-text">
            {room.rounds} rounds
          </span>
        </div>
      </div>

      <div className="mt-4 rounded-xl border border-oa-border bg-black/20 p-4">
        <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Members</p>
        <ul className="mt-2 space-y-2">
          {room.members.map((member) => (
            <li
              key={member.userId}
              className="flex items-center justify-between rounded-lg border border-oa-border bg-black/25 px-3 py-2 text-sm"
            >
              <div>
                <p className="font-medium text-oa-text">{member.displayName}</p>
                <p className="text-xs text-oa-muted">Joined {formatJoinedAt(member.joinedAt)}</p>
              </div>
              <span className="rounded-full border border-oa-border bg-oa-surface-soft/65 px-2.5 py-1 text-xs text-oa-text">
                {member.role}
              </span>
            </li>
          ))}
        </ul>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        <button
          type="button"
          onClick={() => onLeaveRoom(room.id)}
          className="rounded-lg border border-oa-border bg-black/20 px-4 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/45 disabled:cursor-not-allowed disabled:opacity-65"
          disabled={disabled}
        >
          Leave Room
        </button>
        {isHost ? (
          <button
            type="button"
            onClick={() => onCloseRoom(room.id)}
            className="rounded-lg border border-oa-danger/45 bg-oa-danger/15 px-4 py-2 text-sm text-oa-danger transition-colors hover:bg-oa-danger/25 disabled:cursor-not-allowed disabled:opacity-65"
            disabled={disabled}
          >
            Close Room
          </button>
        ) : null}
      </div>

      {isHost ? (
        <p className="mt-3 text-xs text-oa-muted">
          Host rule for OA-PT06: if the host leaves, the room closes and member list is cleared.
        </p>
      ) : null}
    </section>
  );
}
