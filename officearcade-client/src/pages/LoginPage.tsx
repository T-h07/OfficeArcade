import { FormEvent, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../features/auth/AuthContext";

type RedirectState = {
  from?: {
    pathname?: string;
  };
};

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { login } = useAuth();

  const [email, setEmail] = useState("admin@officearcade.local");
  const [password, setPassword] = useState("Admin@123");
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      await login({ email, password });

      const redirectState = location.state as RedirectState | null;
      const nextPath = redirectState?.from?.pathname ?? "/app/dashboard";
      navigate(nextPath, { replace: true });
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Login failed.");
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center px-6 py-10">
      <section className="w-full max-w-4xl overflow-hidden rounded-2xl border border-oa-border bg-oa-surface/90 shadow-glow">
        <div className="grid grid-cols-1 md:grid-cols-[1.1fr_0.9fr]">
          <div className="border-b border-oa-border p-7 md:border-b-0 md:border-r">
            <p className="text-xs uppercase tracking-[0.2em] text-oa-muted">OfficeArcade</p>
            <h1 className="mt-3 text-3xl font-bold text-oa-text">Welcome Back</h1>
            <p className="mt-3 text-sm leading-relaxed text-oa-muted">
              Sign in with your managed company account to access role-specific OfficeArcade controls.
            </p>

            <div className="mt-7 rounded-xl border border-oa-border bg-oa-surface-soft/50 p-4">
              <h2 className="text-sm font-semibold text-oa-text">Development Credentials</h2>
              <ul className="mt-3 space-y-2 text-sm text-oa-muted">
                <li>
                  <span className="font-medium text-oa-text">ADMIN</span>: admin@officearcade.local / Admin@123
                </li>
                <li>
                  <span className="font-medium text-oa-text">EMPLOYEE</span>: employee@officearcade.local /
                  Employee@123
                </li>
              </ul>
            </div>
          </div>

          <div className="p-7">
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div>
                <label htmlFor="email" className="mb-2 block text-sm text-oa-muted">
                  Email
                </label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                  className="w-full rounded-lg border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                  required
                  autoComplete="username"
                />
              </div>

              <div>
                <label htmlFor="password" className="mb-2 block text-sm text-oa-muted">
                  Password
                </label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  className="w-full rounded-lg border border-oa-border bg-black/25 px-3 py-2 text-sm text-oa-text outline-none transition-colors focus:border-oa-accent/60"
                  required
                  autoComplete="current-password"
                />
              </div>

              {errorMessage ? (
                <p className="rounded-lg border border-oa-danger/40 bg-oa-danger/15 px-3 py-2 text-sm text-oa-danger">
                  {errorMessage}
                </p>
              ) : null}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-lg border border-oa-accent/60 bg-oa-accent/25 px-3 py-2 text-sm font-semibold text-oa-text transition-colors hover:bg-oa-accent/35 disabled:cursor-not-allowed disabled:opacity-70"
              >
                {isSubmitting ? "Signing in..." : "Sign In"}
              </button>
            </form>
          </div>
        </div>
      </section>
    </main>
  );
}
