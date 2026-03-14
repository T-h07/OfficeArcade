import { FormEvent, useEffect, useMemo, useState } from "react";
import { GameTypeBadge } from "./GameTypeBadge";
import { getGameTypeVisual } from "./gameTypeVisuals";
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
  const normalizedGameTypeCode = gameTypeCode.toUpperCase();
  const isTwoPlayerLockedGame = normalizedGameTypeCode === "CONNECT_FOUR" || normalizedGameTypeCode === "TRIVIA";
  const isUnoGame = normalizedGameTypeCode === "UNO";
  const selectedGameType = gameTypes.find((gameType) => gameType.code === gameTypeCode);
  const selectedVisual = getGameTypeVisual(gameTypeCode, selectedGameType?.displayName);

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

  useEffect(() => {
    if (!isTwoPlayerLockedGame) {
      if (isUnoGame) {
        setMaxPlayers((previous) => {
          if (previous < 2 || previous > 4) {
            return 4;
          }
          return previous;
        });
      }
      return;
    }
    setMaxPlayers(2);
  }, [isTwoPlayerLockedGame, isUnoGame]);

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
    if (isUnoGame && (maxPlayers < 2 || maxPlayers > 4)) {
      setFormError("UNO rooms must use between 2 and 4 players.");
      return;
    }

    const created = await onSubmit({
      roomName: roomName.trim(),
      gameTypeCode,
      maxPlayers: isTwoPlayerLockedGame ? 2 : isUnoGame ? Math.min(Math.max(maxPlayers, 2), 4) : maxPlayers,
      rounds,
      isPrivate,
      password: isPrivate ? password.trim() : undefined
    });

    if (!created) {
      return;
    }

    setRoomName("Break Room");
    setMaxPlayers(isTwoPlayerLockedGame ? 2 : isUnoGame ? 4 : 4);
    setRounds(3);
    setIsPrivate(false);
    setPassword("");
  }

  return (
    <section className={selectedVisual.surfaceClassName}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-oa-muted">Room Launch Panel</p>
          <h2 className="mt-1 text-xl font-semibold text-oa-text">Host a Room</h2>
          <p className="mt-1 text-sm text-oa-muted">Stage a session and invite players into a live match.</p>
        </div>
        {gameTypeCode ? <GameTypeBadge gameTypeCode={gameTypeCode} displayName={selectedGameType?.displayName} /> : null}
      </div>

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
            className="oa-input"
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
              className="oa-select"
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
              className="oa-input"
              min={2}
              max={isUnoGame ? 4 : 8}
              disabled={disabled || isTwoPlayerLockedGame}
              required
            />
            {isTwoPlayerLockedGame ? (
              <p className="mt-1 text-xs text-oa-muted">Connect Four and Trivia rooms are fixed to 2 players.</p>
            ) : null}
            {isUnoGame ? (
              <p className="mt-1 text-xs text-oa-muted">UNO rooms support 2 to 4 players.</p>
            ) : null}
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
              className="oa-input"
              min={1}
              max={10}
              disabled={disabled}
              required
            />
          </div>

          <div className="oa-panel-soft px-3 py-2">
            <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Visibility</p>
            <div className="mt-2 flex items-center gap-2">
              <button
                type="button"
                onClick={() => setIsPrivate(false)}
                className={`oa-btn px-3 py-1 text-xs ${
                  !isPrivate
                    ? "oa-btn-primary"
                    : "oa-btn-ghost"
                }`}
                disabled={disabled}
              >
                Public
              </button>
              <button
                type="button"
                onClick={() => setIsPrivate(true)}
                className={`oa-btn px-3 py-1 text-xs ${
                  isPrivate
                    ? "oa-btn-primary"
                    : "oa-btn-ghost"
                }`}
                disabled={disabled}
              >
                Private
              </button>
            </div>
            <p className="mt-2 text-xs text-oa-muted">
              {isPrivate ? "Password-protected invite room" : "Open to all eligible players"}
            </p>
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
              className="oa-input"
              minLength={4}
              maxLength={72}
              placeholder="Required for private rooms"
              disabled={disabled}
              required={isPrivate}
            />
          </div>
        ) : null}

        {formError ? (
          <p className="oa-alert oa-alert-danger">
            {formError}
          </p>
        ) : null}

        <button
          type="submit"
          className="oa-btn oa-btn-primary w-full px-3 py-2"
          disabled={disabled || gameTypes.length === 0}
        >
          Launch {selectedVisual.shortLabel} Room
        </button>
      </form>
    </section>
  );
}
