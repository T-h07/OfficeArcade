import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { getNavItemsForRole } from "../navigation/navItems";
import { NotificationBell } from "../notifications/components/NotificationBell";

type RouteAccent = "dashboard" | "play" | "challenges" | "leaderboards" | "notifications" | "identity" | "admin";

function resolveRouteAccent(pathname: string): RouteAccent {
  if (pathname.startsWith("/app/play")) {
    return "play";
  }
  if (pathname.startsWith("/app/challenges")) {
    return "challenges";
  }
  if (pathname.startsWith("/app/leaderboards")) {
    return "leaderboards";
  }
  if (pathname.startsWith("/app/notifications")) {
    return "notifications";
  }
  if (pathname.startsWith("/app/store") || pathname.startsWith("/app/inventory") || pathname.startsWith("/app/profile")) {
    return "identity";
  }
  if (pathname.startsWith("/app/admin")) {
    return "admin";
  }
  return "dashboard";
}

function resolveNavLinkClassName(isActive: boolean) {
  if (isActive) {
    return "oa-nav-link oa-nav-link-active";
  }
  return "oa-nav-link";
}

export function AppShell() {
  const location = useLocation();
  const { accessToken, user, logout } = useAuth();

  if (!user) {
    return null;
  }

  const navItems = getNavItemsForRole(user.role);
  const departmentLabel = user.department
    ? `${user.department.displayName} (${user.department.code})`
    : "Unassigned";
  const routeAccent = resolveRouteAccent(location.pathname);
  const routeLabel = location.pathname.startsWith("/app/")
    ? location.pathname.replace("/app/", "").replace(/\//g, " / ")
    : "dashboard";

  return (
    <div className="flex min-h-screen" data-route-accent={routeAccent}>
      <aside className="hidden w-64 border-r border-[color:var(--oa-shell-border)] bg-[color:var(--oa-shell-soft)] p-4 backdrop-blur-md md:flex md:flex-col">
        <div>
          <p className="text-xs uppercase tracking-[0.24em] text-oa-muted">OfficeArcade</p>
          <h1 className="mt-2 text-2xl font-semibold text-oa-text">Arcade Control</h1>
          <p className="mt-1 text-xs text-oa-muted">Multiplayer workspace</p>
        </div>

        <nav className="mt-6 flex flex-col gap-2">
          {navItems.map((item) => (
            <NavLink key={item.to} to={item.to} className={({ isActive }) => resolveNavLinkClassName(isActive)}>
              {item.label}
            </NavLink>
          ))}
        </nav>

        <div className="oa-panel-soft mt-auto">
          <p className="text-xs text-oa-muted">Signed in</p>
          <p className="mt-1 text-sm font-medium text-oa-text">{user.displayName}</p>
          <p className="text-xs text-oa-muted">{user.role}</p>
          <p className="mt-1 text-xs text-oa-muted">Department: {departmentLabel}</p>
        </div>
      </aside>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="flex items-center justify-between border-b border-[color:var(--oa-shell-border)] bg-[color:var(--oa-shell-soft)] px-4 py-3 backdrop-blur-md md:px-6">
          <div>
            <p className="text-sm text-oa-muted">OfficeArcade Control Layer</p>
            <p className="text-sm font-medium text-oa-text">{user.email}</p>
          </div>

          <div className="flex items-center gap-3">
            <span className="oa-chip oa-chip-route hidden md:inline-flex">{routeLabel}</span>
            <NotificationBell
              accessToken={accessToken}
              userId={user.id}
              onUnauthorized={logout}
            />
            <span className="oa-chip">
              {user.role}
            </span>
            <span
              className={`oa-chip ${
                user.department?.active
                  ? "oa-chip-route"
                  : ""
              }`}
            >
              {departmentLabel}
            </span>
            <button
              type="button"
              className="oa-btn oa-btn-secondary"
              onClick={logout}
            >
              Logout
            </button>
          </div>
        </header>

        <nav className="flex flex-wrap gap-2 border-b border-[color:var(--oa-shell-border)] bg-[color:var(--oa-shell-soft)] p-3 md:hidden">
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
