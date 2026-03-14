import { FormEvent, useEffect, useState } from "react";

type JoinPrivateRoomModalProps = {
  isOpen: boolean;
  roomName: string;
  isSubmitting: boolean;
  onClose: () => void;
  onSubmit: (password: string) => Promise<boolean>;
};

export function JoinPrivateRoomModal({
  isOpen,
  roomName,
  isSubmitting,
  onClose,
  onSubmit
}: JoinPrivateRoomModalProps) {
  const [password, setPassword] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen) {
      setPassword("");
      setErrorMessage(null);
    }
  }, [isOpen]);

  if (!isOpen) {
    return null;
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setErrorMessage(null);

    if (password.trim().length < 4) {
      setErrorMessage("Password must be at least 4 characters.");
      return;
    }

    const joined = await onSubmit(password.trim());
    if (!joined) {
      return;
    }
    onClose();
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 px-4">
      <section className="w-full max-w-md rounded-2xl border border-oa-border bg-oa-surface p-5 shadow-glow">
        <h2 className="text-lg font-semibold text-oa-text">Join Private Room</h2>
        <p className="mt-1 text-sm text-oa-muted">{roomName}</p>

        <form className="mt-4 space-y-3" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="private-room-password" className="mb-1 block text-xs uppercase tracking-[0.12em] text-oa-muted">
              Password
            </label>
            <input
              id="private-room-password"
              type="password"
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              className="w-full rounded-lg border border-oa-border bg-black/30 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/55"
              minLength={4}
              maxLength={72}
              autoFocus
              required
              disabled={isSubmitting}
            />
          </div>

          {errorMessage ? (
            <p className="rounded-lg border border-oa-danger/40 bg-oa-danger/10 px-3 py-2 text-sm text-oa-danger">
              {errorMessage}
            </p>
          ) : null}

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="rounded-lg border border-oa-border bg-black/20 px-3 py-2 text-sm text-oa-muted transition-colors hover:border-oa-accent/45 hover:text-oa-text"
              disabled={isSubmitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="rounded-lg border border-oa-accent/55 bg-oa-accent/25 px-3 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/35 disabled:cursor-not-allowed disabled:opacity-70"
              disabled={isSubmitting}
            >
              Join Room
            </button>
          </div>
        </form>
      </section>
    </div>
  );
}
