import { FormEvent, useEffect, useMemo, useState } from "react";
import type { CreateLobbyRoomRequest, LobbyGameType } from "../types/lobby.types";

type CreateRoomFormProps = {
  gameTypes: LobbyGameType[];
  disabled: boolean;
  onSubmit: (request: CreateLobbyRoomRequest) => Promise<boolean>;
};

export function CreateRoomForm({ gameTypes, disabled, onSubmit }: CreateRoomFormProps) {
  const defaultGameType = useMemo(() => {
    if (gameTypes.length === 0) {
      return "";
    }
    return gameTypes[0].code;
  }, [gameTypes]);

  const [roomName, setRoomName] = useState("Break Room");
  const [gameTypeCode, setGameTypeCode] = useState(defaultGameType);
  const [maxPlayers, setMaxPlayers] = useState(4);
  const [rounds, setRounds] = useState(3);
  const [isPrivate, setIsPrivate] = useState(false);
  const [password, setPassword] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (!defaultGameType) {
      setGameTypeCode("");
      return;
    }
    if (gameTypes.some((gameType) => gameType.code === gameTypeCode)) {
      return;
    }
    setGameTypeCode(defaultGameType);
  }, [defaultGameType, gameTypeCode, gameTypes]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    if (!gameTypeCode) {
      setFormError("Select an enabled game type.");
      return;
    }
    if (roomName.trim().length < 3) {
      setFormError("Room name must be at least 3 characters.");
      return;
    }
    if (isPrivate && password.trim().length < 4) {
      setFormError("Private room password must be at least 4 characters.");
      return;
    }

    const created = await onSubmit({
      roomName: roomName.trim(),
      gameTypeCode,
      maxPlayers,
      rounds,
      isPrivate,
      password: isPrivate ? password.trim() : undefined
    });

    if (!created) {
      return;
    }

    setRoomName("Break Room");
    setMaxPlayers(4);
    setRounds(3);
    setIsPrivate(false);
    setPassword("");
  }

  return (
    <section className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
      <h2 className="text-lg font-semibold text-oa-text">Host a Room</h2>
      <p className="mt-1 text-sm text-oa-muted">Create a persisted lobby room for short office game sessions.</p>

      <form className="mt-4 space-y-3" onSubmit={handleSubmit}>
        <div>
          <label htmlFor="room-name" className="mb-1 block text-xs uppercase tracking-[0.12em] text-oa-muted">
            Room Name
          </label>
          <input
            id="room-name"
            type="text"
            value={roomName}
            onChange={(event) => setRoomName(event.target.value)}
            className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/55"
            placeholder="Friday Break Match"
            minLength={3}
            maxLength={80}
            disabled={disabled}
            required
          />
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="game-type" className="mb-1 block text-xs uppercase tracking-[0.12em] text-oa-muted">
              Game Type
            </label>
            <select
              id="game-type"
              value={gameTypeCode}
              onChange={(event) => setGameTypeCode(event.target.value)}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/55"
              disabled={disabled || gameTypes.length === 0}
              required
            >
              {gameTypes.map((gameType) => (
                <option key={gameType.id} value={gameType.code}>
                  {gameType.displayName}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label htmlFor="max-players" className="mb-1 block text-xs uppercase tracking-[0.12em] text-oa-muted">
              Max Players
            </label>
            <input
              id="max-players"
              type="number"
              value={maxPlayers}
              onChange={(event) => setMaxPlayers(Number(event.target.value))}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/55"
              min={2}
              max={8}
              disabled={disabled}
              required
            />
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label htmlFor="rounds" className="mb-1 block text-xs uppercase tracking-[0.12em] text-oa-muted">
              Rounds
            </label>
            <input
              id="rounds"
              type="number"
              value={rounds}
              onChange={(event) => setRounds(Number(event.target.value))}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/55"
              min={1}
              max={10}
              disabled={disabled}
              required
            />
          </div>

          <div className="rounded-lg border border-oa-border bg-black/20 px-3 py-2">
            <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Visibility</p>
            <div className="mt-2 flex items-center gap-2">
              <button
                type="button"
                onClick={() => setIsPrivate(false)}
                className={`rounded-md px-3 py-1 text-xs ${
                  !isPrivate
                    ? "border border-oa-accent/55 bg-oa-accent/20 text-oa-text"
                    : "border border-oa-border bg-black/20 text-oa-muted"
                }`}
                disabled={disabled}
              >
                Public
              </button>
              <button
                type="button"
                onClick={() => setIsPrivate(true)}
                className={`rounded-md px-3 py-1 text-xs ${
                  isPrivate
                    ? "border border-oa-accent/55 bg-oa-accent/20 text-oa-text"
                    : "border border-oa-border bg-black/20 text-oa-muted"
                }`}
                disabled={disabled}
              >
                Private
              </button>
            </div>
          </div>
        </div>

        {isPrivate ? (
          <div>
            <label htmlFor="room-password" className="mb-1 block text-xs uppercase tracking-[0.12em] text-oa-muted">
              Room Password
            </label>
            <input
              id="room-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/55"
              minLength={4}
              maxLength={72}
              placeholder="Required for private rooms"
              disabled={disabled}
              required={isPrivate}
            />
          </div>
        ) : null}

        {formError ? (
          <p className="rounded-lg border border-oa-danger/40 bg-oa-danger/10 px-3 py-2 text-sm text-oa-danger">
            {formError}
          </p>
        ) : null}

        <button
          type="submit"
          className="w-full rounded-lg border border-oa-accent/55 bg-oa-accent/25 px-3 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/35 disabled:cursor-not-allowed disabled:opacity-70"
          disabled={disabled || gameTypes.length === 0}
        >
          Create Room
        </button>
      </form>
    </section>
  );
}
