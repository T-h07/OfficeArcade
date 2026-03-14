import { useAuth } from "../features/auth/AuthContext";

type PagePlaceholderProps = {
  title: string;
  description: string;
  futureNote: string;
};

export function PagePlaceholder({ title, description, futureNote }: PagePlaceholderProps) {
  const { user } = useAuth();

  return (
    <section className="space-y-4">
      <header>
        <h1 className="text-2xl font-semibold text-oa-text">{title}</h1>
        <p className="mt-1 text-sm text-oa-muted">{description}</p>
      </header>

      <article className="rounded-2xl border border-oa-border bg-oa-surface/80 p-5">
        <p className="text-sm text-oa-text">{futureNote}</p>
        {user ? (
          <div className="mt-4 rounded-lg border border-oa-border bg-oa-surface-soft/50 p-3 text-xs text-oa-muted">
            Signed in as <span className="font-semibold text-oa-text">{user.displayName}</span> ({user.email}) with
            role <span className="font-semibold text-oa-text">{user.role}</span>.
          </div>
        ) : null}
      </article>
    </section>
  );
}
