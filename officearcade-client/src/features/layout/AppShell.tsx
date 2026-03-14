import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { getNavItemsForRole } from "../navigation/navItems";
import { NotificationBell } from "../notifications/components/NotificationBell";

function resolveNavLinkClassName(isActive: boolean) {
  if (isActive) {
    return "rounded-lg border border-oa-accent/40 bg-oa-accent/20 px-3 py-2 text-sm font-medium text-oa-text";
  }
  return "rounded-lg border border-transparent px-3 py-2 text-sm text-oa-muted transition-colors hover:border-oa-border hover:bg-oa-surface-soft/60 hover:text-oa-text";
}

export function AppShell() {
  const { accessToken, user, logout } = useAuth();

  if (!user) {
    return null;
  }

  const navItems = getNavItemsForRole(user.role);

  return (
    <div className="flex min-h-screen">
      <aside className="hidden w-64 border-r border-oa-border bg-black/30 p-4 md:flex md:flex-col">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-oa-muted">OfficeArcade</p>
          <h1 className="mt-2 text-xl font-semibold text-oa-text">Control Shell</h1>
        </div>

        <nav className="mt-6 flex flex-col gap-2">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => resolveNavLinkClassName(isActive)}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="mt-auto rounded-xl border border-oa-border bg-oa-surface/70 p-3">
          <p className="text-xs text-oa-muted">Signed in</p>
          <p className="mt-1 text-sm font-medium text-oa-text">{user.displayName}</p>
          <p className="text-xs text-oa-muted">{user.role}</p>
        </div>
      </aside>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-oa-border bg-black/25 px-4 py-3 md:px-6">
          <div>
            <p className="text-sm text-oa-muted">OfficeArcade App Shell</p>
            <p className="text-sm font-medium text-oa-text">{user.email}</p>
          </div>

          <div className="flex items-center gap-3">
            <NotificationBell
              accessToken={accessToken}
              userId={user.id}
              onUnauthorized={logout}
            />
            <span className="rounded-full border border-oa-border bg-oa-surface px-3 py-1 text-xs font-medium text-oa-text">
              {user.role}
            </span>
            <button
              type="button"
              className="rounded-lg border border-oa-border bg-oa-surface px-3 py-2 text-sm text-oa-text transition-colors hover:border-oa-accent/40 hover:text-white"
              onClick={logout}
            >
              Logout
            </button>
          </div>
        </header>

        <nav className="flex flex-wrap gap-2 border-b border-oa-border bg-black/20 p-3 md:hidden">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => resolveNavLinkClassName(isActive)}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <main className="flex-1 p-4 md:p-6">
          <div className="mx-auto w-full max-w-5xl">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
