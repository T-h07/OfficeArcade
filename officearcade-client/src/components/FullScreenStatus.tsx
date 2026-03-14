type FullScreenStatusProps = {
  title: string;
  message: string;
};

export function FullScreenStatus({ title, message }: FullScreenStatusProps) {
  return (
    <main className="flex min-h-screen items-center justify-center px-6 py-10">
      <section className="w-full max-w-md rounded-2xl border border-oa-border bg-oa-surface/90 p-6 text-center shadow-glow">
        <h1 className="text-xl font-semibold text-oa-text">{title}</h1>
        <p className="mt-2 text-sm text-oa-muted">{message}</p>
      </section>
    </main>
  );
}
