import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { getNavItemsForRole, type NavItem, type NavSection } from "../navigation/navItems";
import { NotificationBell } from "../notifications/components/NotificationBell";
import { getShellRouteMeta } from "./shellRouteMeta";

const SECTION_ORDER: NavSection[] = ["CORE", "SOCIAL", "IDENTITY", "OPERATIONS", "SYSTEM"];

const SECTION_LABELS: Record<NavSection, string> = {
  CORE: "Core Play",
  SOCIAL: "Competition",
  IDENTITY: "Profile & Rewards",
  OPERATIONS: "Admin Operations",
  SYSTEM: "System"
};

function resolveNavLinkClassName(isActive: boolean) {
  return isActive ? "oa-shell-nav-link oa-shell-nav-link-active" : "oa-shell-nav-link";
}

function groupNavItems(navItems: NavItem[]) {
  return SECTION_ORDER.map((section) => ({
    section,
    title: SECTION_LABELS[section],
    items: navItems.filter((item) => item.section === section)
  })).filter((entry) => entry.items.length > 0);
}

export function AppShell() {
  const location = useLocation();
  const { accessToken, user, logout } = useAuth();

  if (!user) {
    return null;
  }

  const navItems = getNavItemsForRole(user.role);
  const groupedNavItems = groupNavItems(navItems);
  const departmentLabel = user.department ? `${user.department.displayName} (${user.department.code})` : "Unassigned";
  const routeMeta = getShellRouteMeta(location.pathname);
  const contentWrapClass =
    routeMeta.mode === "admin"
      ? "oa-shell-content-wrap oa-shell-content-wrap-admin"
      : "oa-shell-content-wrap oa-shell-content-wrap-player";

  return (
    <div className="oa-shell-root flex min-h-screen" data-route-accent={routeMeta.accent} data-shell-mode={routeMeta.mode}>
      <aside className="oa-shell-sidebar hidden w-[272px] md:flex md:flex-col">
        <div className="rounded-xl border border-[color:var(--oa-shell-border)] bg-black/20 p-3">
          <p className="text-[10px] uppercase tracking-[0.28em] text-oa-muted">OfficeArcade</p>
          <h1 className="mt-1 text-2xl font-semibold text-oa-text">Control Room</h1>
          <p className="mt-1 text-xs text-oa-muted">Live multiplayer workspace</p>
          <div className="mt-3 flex items-center gap-2">
            <span className={`oa-chip ${routeMeta.mode === "admin" ? "oa-chip-info" : "oa-chip-route"}`}>
              {routeMeta.mode === "admin" ? "Admin Mode" : "Player Mode"}
            </span>
            <span className="oa-chip">{user.role}</span>
          </div>
        </div>

        <nav className="mt-5 flex-1 overflow-y-auto pr-1">
          {groupedNavItems.map((group) => (
            <section key={group.section} className="oa-shell-nav-group">
              <p className="oa-shell-nav-group-title">{group.title}</p>
              {group.items.map((item) => (
                <NavLink key={item.to} to={item.to} className={({ isActive }) => resolveNavLinkClassName(isActive)}>
                  {item.label}
                </NavLink>
              ))}
            </section>
          ))}
        </nav>

        <div className="oa-panel-soft mt-4">
          <p className="text-xs uppercase tracking-[0.12em] text-oa-muted">Signed in</p>
          <p className="mt-1 text-sm font-semibold text-oa-text">{user.displayName}</p>
          <p className="text-xs text-oa-muted">{user.email}</p>
          <div className="mt-2 flex flex-wrap gap-2">
            <span className="oa-chip">{user.role}</span>
            <span className={`oa-chip ${user.department?.active ? "oa-chip-route" : ""}`}>{departmentLabel}</span>
          </div>
        </div>
      </aside>

      <div className="flex min-h-screen flex-1 flex-col">
        <header className="oa-shell-topbar">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs uppercase tracking-[0.18em] text-oa-muted">{routeMeta.mode === "admin" ? "Operations View" : "Player View"}</p>
              <p className="text-lg font-semibold text-oa-text">{routeMeta.label}</p>
              <p className="text-xs text-oa-muted">{routeMeta.hint}</p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <span className="oa-chip oa-chip-route hidden lg:inline-flex">{routeMeta.label}</span>
              <NotificationBell accessToken={accessToken} userId={user.id} onUnauthorized={logout} />
              <span className={`oa-chip ${routeMeta.mode === "admin" ? "oa-chip-info" : "oa-chip-success"}`}>
                {routeMeta.mode === "admin" ? "Ops" : "Live"}
              </span>
              <span className={`oa-chip ${user.department?.active ? "oa-chip-route" : ""}`}>{departmentLabel}</span>
              <button type="button" className="oa-btn oa-btn-secondary" onClick={logout}>
                Logout
              </button>
            </div>
          </div>
        </header>

        <nav className="border-b border-[color:var(--oa-shell-border)] bg-[color:var(--oa-shell-soft)] p-3 md:hidden">
          <div className="flex flex-wrap gap-2">
            {navItems.map((item) => (
              <NavLink key={item.to} to={item.to} className={({ isActive }) => resolveNavLinkClassName(isActive)}>
                {item.label}
              </NavLink>
            ))}
          </div>
        </nav>

        <main className="oa-shell-content">
          <div className={contentWrapClass}>
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
